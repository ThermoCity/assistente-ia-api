package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.dto.OrientacaoResponse;
import br.com.thermocity.assistenteiaapi.dto.PerfilContato;
import br.com.thermocity.assistenteiaapi.dto.RefugioIndicado;
import br.com.thermocity.assistenteiaapi.dto.RoteiroIa;
import br.com.thermocity.assistenteiaapi.exception.PessoaNaoEncontradaException;
import br.com.thermocity.assistenteiaapi.model.OrientacaoContato;
import br.com.thermocity.assistenteiaapi.model.Pessoa;
import br.com.thermocity.assistenteiaapi.repository.OrientacaoContatoRepository;
import br.com.thermocity.assistenteiaapi.repository.PessoaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class OrientacaoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final OrientacaoContatoRepository orientacoes;
    private final PessoaRepository pessoas;
    private final ListaDoDiaService listaDoDia;
    private final RefugioService refugios;
    private final BuscaGuiasService buscaGuias;
    private final RoteiroIaService roteiroIa;
    private final String modelo;

    public OrientacaoService(OrientacaoContatoRepository orientacoes, PessoaRepository pessoas,
                             ListaDoDiaService listaDoDia, RefugioService refugios,
                             BuscaGuiasService buscaGuias, RoteiroIaService roteiroIa,
                             @Value("${spring.ai.openai.chat.options.model:}") String modelo) {
        this.orientacoes = orientacoes;
        this.pessoas = pessoas;
        this.listaDoDia = listaDoDia;
        this.refugios = refugios;
        this.buscaGuias = buscaGuias;
        this.roteiroIa = roteiroIa;
        this.modelo = modelo;
    }

    public OrientacaoResponse orientacaoDeHoje(Long pessoaId) {
        return orientacaoDoDia(pessoaId, LocalDate.now(FUSO));
    }

    OrientacaoResponse orientacaoDoDia(Long pessoaId, LocalDate hoje) {
        // Cenário 2: já gerada hoje -> devolve a guardada, sem chamar o modelo
        return orientacoes.findByPessoaIdAndDataLista(pessoaId, hoje)
                .map(OrientacaoService::paraResposta)
                .orElseGet(() -> gerar(pessoaId, hoje));
    }

    private OrientacaoResponse gerar(Long pessoaId, LocalDate hoje) {
        Pessoa pessoa = pessoas.findById(pessoaId).orElseThrow(() -> new PessoaNaoEncontradaException(pessoaId));
        // Só quem está na lista de busca ativa de hoje recebe roteiro; o nível vem da própria lista
        ListaDoDiaService.PessoaNaLista naLista = listaDoDia.daPessoa(pessoaId, hoje);
        int nivel = naLista.nivelCalor();
        // O índice de calor previsto virá da API de clima (.NET); a lista só guarda o nível
        Double icPrevistoC = null;
        RefugioIndicado refugio = refugios.maisProximo(pessoa.getLat(), pessoa.getLon()).orElse(null);

        // Só o necessário vai para o modelo: nada de nome, telefone, endereço ou coordenadas
        PerfilContato perfil = new PerfilContato(
                pessoa.getIdade(),
                pessoa.isMoraSozinho(),
                pessoa.isMobilidadeReduzida(),
                pessoa.isCondicaoCronica(),
                nivel,
                icPrevistoC,
                refugio == null ? null : refugio.nome(),
                refugio == null ? null : refugio.distanciaMetros(),
                naLista.motivos());

        try {
            List<Document> trechos = buscaGuias.buscar(perfil);
            if (trechos == null || trechos.isEmpty()) {
                throw new IllegalStateException("Nenhum trecho dos guias oficiais foi encontrado");
            }
            RoteiroIa roteiro = roteiroIa.gerar(perfil, trechos);

            // Cenário 1: guarda junto da lista do dia e devolve
            OrientacaoContato orientacao = orientacoes.save(OrientacaoContato.builder()
                    .pessoaId(pessoaId)
                    .dataLista(hoje)
                    .nivelCalor(nivel)
                    .icPrevistoC(icPrevistoC)
                    .roteiro(String.join("\n", roteiro.roteiro()))
                    .sinaisAlerta(String.join("\n", roteiro.sinaisAlerta() == null ? List.of() : roteiro.sinaisAlerta()))
                    .refugioNome(refugio == null ? null : refugio.nome())
                    .refugioEndereco(refugio == null ? null : refugio.endereco())
                    .refugioDistanciaMetros(refugio == null ? null : refugio.distanciaMetros())
                    .fontes(String.join("\n", fontesDosTrechos(trechos)))
                    .modelo(modelo)
                    .geradoEm(OffsetDateTime.now(FUSO))
                    .build());
            return paraResposta(orientacao);
        } catch (RuntimeException e) {
            // Cenário 3: a IA falhou -> orientação padrão do nível, sem bloquear o atendimento.
            // Não é guardada, para que a próxima abertura tente a IA de novo.
            log.warn("IA indisponível para a pessoa {}: {}. Usando a orientação padrão do nível.", pessoaId, e.getMessage());
            return new OrientacaoResponse(
                    pessoaId,
                    hoje,
                    nivel,
                    OrientacaoPadrao.nomeDoNivel(nivel),
                    icPrevistoC,
                    OrientacaoPadrao.roteiro(perfil),
                    OrientacaoPadrao.SINAIS_ALERTA,
                    refugio,
                    OrientacaoPadrao.FONTES,
                    true,
                    "Gerada sem IA: orientação padrão do nível. " + OrientacaoPadrao.AVISO,
                    OffsetDateTime.now(FUSO));
        }
    }

    // Guias de onde vieram os trechos, sem repetir (metadado "fonte" gravado pelo IngestaoService)
    static List<String> fontesDosTrechos(List<Document> trechos) {
        return trechos.stream()
                .map(t -> t.getMetadata().get("fonte"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .toList();
    }

    private static OrientacaoResponse paraResposta(OrientacaoContato o) {
        RefugioIndicado refugio = o.getRefugioNome() == null
                ? null
                : new RefugioIndicado(o.getRefugioNome(), o.getRefugioEndereco(), o.getRefugioDistanciaMetros());
        return new OrientacaoResponse(
                o.getPessoaId(),
                o.getDataLista(),
                o.getNivelCalor(),
                OrientacaoPadrao.nomeDoNivel(o.getNivelCalor()),
                o.getIcPrevistoC(),
                linhas(o.getRoteiro()),
                linhas(o.getSinaisAlerta()),
                refugio,
                linhas(o.getFontes()),
                false,
                OrientacaoPadrao.AVISO,
                o.getGeradoEm());
    }

    private static List<String> linhas(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split("\n")).filter(l -> !l.isBlank()).toList();
    }
}