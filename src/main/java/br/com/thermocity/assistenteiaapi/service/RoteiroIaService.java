package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.dto.PerfilContato;
import br.com.thermocity.assistenteiaapi.dto.RoteiroIa;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

// Parte "G" do RAG: o modelo escreve o roteiro a partir do perfil (sem identificação) e dos trechos recuperados
@Service
public class RoteiroIaService {

    private static final String SYSTEM_PROMPT = """
            Você é o assistente de busca ativa do ThermoCity. Você ajuda agentes de saúde de uma UBS
            de Presidente Prudente-SP a fazer um contato por telefone com uma pessoa idosa num dia de calor.

            Fontes:
            - Use somente as informações dos trechos dos guias oficiais enviados na mensagem.
              Cada trecho começa com a referência completa do guia, no formato [Fonte: ...].
            - Ao final de cada passo ou sinal apoiado num trecho, cite a fonte de forma curta, só com o título
              do guia, sem instituição e sem data. Exemplos: [Nota Técnica nº 18/2023-SVSA/MS], [Heat and Medications – CDC].
            - Não cite fonte para o que vem dos dados da pessoa ou do refúgio (nome, distância, UBS):
              esses dados não estão nos guias.
            - Não invente quantidades de água, doses, temperaturas, telefones ou serviços que não estejam nos trechos
              ou nos dados da pessoa. O único telefone que você pode citar é o 192 (SAMU).

            Limites:
            - Não faça diagnóstico e não prescreva, suspenda nem altere medicamentos.
            - Diante de sinais graves (pele muito quente, confusão mental, desmaio ou convulsão), a conduta é ligar 192 (SAMU).
              Diante de sinais leves, procurar a UBS.
            - Oferecer líquidos só se a pessoa estiver consciente e orientada: isso é uma checagem que o agente faz,
              não uma frase para dizer à pessoa. Não escreva "se a senhora estiver consciente" no roteiro.

            O que devolver:
            - roteiro: de 5 a 8 passos curtos, na ordem da ligação: como se apresentar (use o nome da UBS informado
              e deixe só [nome do agente] para o agente completar); o que perguntar; os cuidados adequados ao nível de
              calor e às condições da pessoa; a indicação do refúgio informado (nome e distância), se houver;
              e quando acionar a equipe da UBS ou o SAMU.
            - sinaisAlerta: de 3 a 5 itens no formato "Sinal: conduta.". Junte num item só os sinais que têm a mesma
              conduta (ex.: "Pele muito quente, confusão mental, desmaio ou convulsão: ligar 192 (SAMU) imediatamente.").
            - Quanto maior o nível de calor e mais condições de risco a pessoa tiver, mais firme deve ser a orientação.

            Estilo:
            - Português do Brasil, frases curtas e simples, fáceis de ler em voz alta.
            - Trate a pessoa como indicado em "Tratamento" nos dados (por exemplo, "a senhora"), nunca "o senhor/a senhora".
            - Texto puro: sem markdown, sem asteriscos, negrito, títulos ou marcadores no início dos itens.
            """;

    private final ChatClient chatClient;

    public RoteiroIaService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT) // papel e regras do roteiro
                .defaultAdvisors(new SimpleLoggerAdvisor()) // loga o pedido e a resposta
                .build();
    }

    public RoteiroIa gerar(PerfilContato perfil, List<Document> trechos) {
        RoteiroIa roteiro = chatClient.prompt()
                .user(montarPrompt(perfil, trechos))
                .call()
                .entity(RoteiroIa.class); // saída estruturada: roteiro + sinais de alerta

        if (roteiro == null || roteiro.roteiro() == null || roteiro.roteiro().isEmpty()) {
            throw new IllegalStateException("O modelo não devolveu um roteiro");
        }
        return new RoteiroIa(limpar(roteiro.roteiro()),
                roteiro.sinaisAlerta() == null ? List.of() : limpar(roteiro.sinaisAlerta()));
    }

    static List<String> limpar(List<String> linhas) {
        return linhas.stream()
                .filter(Objects::nonNull)
                .map(l -> l.replace("**", "").replace("__", "")
                        .replaceFirst("^\\s*([-*•]|\\d+[.)])\\s+", "")
                        .trim())
                .filter(l -> !l.isEmpty())
                .toList();
    }

    static String montarPrompt(PerfilContato perfil, List<Document> trechos) {
        StringBuilder texto = new StringBuilder("Dados da pessoa (sem identificação):\n");
        texto.append("- Idade: ").append(perfil.idade() == null ? "60 anos ou mais" : perfil.idade() + " anos").append("\n");
        texto.append("- Tratamento: ").append(perfil.tratamento()).append("\n");
        if (perfil.ubsNome() != null) {
            texto.append("- UBS que faz o contato: ").append(perfil.ubsNome()).append("\n");
        }
        texto.append("- Mora sozinha: ").append(simNao(perfil.moraSozinha())).append("\n");
        texto.append("- Mobilidade reduzida: ").append(simNao(perfil.mobilidadeReduzida())).append("\n");
        texto.append("- Doença crônica: ").append(simNao(perfil.condicaoCronica())).append("\n");
        if (perfil.motivosPrioridade() != null && !perfil.motivosPrioridade().isEmpty()) {
            texto.append("- Por que está na lista de busca ativa de hoje: ")
                    .append(String.join("; ", perfil.motivosPrioridade())).append("\n");
        }
        texto.append("\n");

        texto.append("Calor hoje: NC").append(perfil.nivelCalor())
                .append(" - ").append(OrientacaoPadrao.nomeDoNivel(perfil.nivelCalor()));
        if (perfil.icPrevistoC() != null) {
            texto.append(" (índice de calor máximo previsto: ").append(perfil.icPrevistoC()).append(" °C)");
        }
        texto.append("\n");

        if (perfil.refugioNome() != null) {
            texto.append("Refúgio mais próximo: ").append(perfil.refugioNome());
            if (perfil.refugioDistanciaMetros() != null) {
                texto.append(", a ").append(OrientacaoPadrao.distancia(perfil.refugioDistanciaMetros()));
            }
            texto.append("\n");
        } else {
            texto.append("Refúgio mais próximo: não encontrado\n");
        }

        texto.append("\nTrechos dos guias oficiais:\n");
        for (Document trecho : trechos) {
            texto.append("\n").append(trecho.getText()).append("\n");
        }
        return texto.toString();
    }

    private static String simNao(boolean valor) {
        return valor ? "sim" : "não";
    }
}