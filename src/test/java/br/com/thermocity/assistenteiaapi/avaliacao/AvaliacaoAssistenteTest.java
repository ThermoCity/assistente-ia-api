package br.com.thermocity.assistenteiaapi.avaliacao;

import br.com.thermocity.assistenteiaapi.dto.RoteiroIa;
import br.com.thermocity.assistenteiaapi.service.BuscaGuiasService;
import br.com.thermocity.assistenteiaapi.service.ChatService;
import br.com.thermocity.assistenteiaapi.service.RoteiroIaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Avaliação do assistente com o modelo de verdade (PBI 60). NÃO roda no "mvnw test" comum: chama o Gemini
 * 18 vezes. Para rodar (com DB_* e GEMINI_API_KEY definidas), ligue com -Davaliacao=true
 * (ou a variável de ambiente AVALIACAO_IA=true):
 * <pre>
 * .\mvnw.cmd test -Dtest=AvaliacaoAssistenteTest -Davaliacao=true
 * </pre>
 * Gera em target/avaliacao-ia: resultados.md (tabela dos critérios), resultados.csv e respostas.md (texto
 * completo de cada resposta, para a leitura humana). Não grava nada no banco.
 */
@SpringBootTest
@EnabledIf("avaliacaoLigada")
class AvaliacaoAssistenteTest {

    /** -Davaliacao=true (funciona no "Execute Maven goal" do IntelliJ) ou a variável AVALIACAO_IA=true. */
    static boolean avaliacaoLigada() {
        return "true".equalsIgnoreCase(System.getProperty("avaliacao"))
                || "true".equalsIgnoreCase(System.getenv("AVALIACAO_IA"));
    }

    private static final Path SAIDA = Path.of("target", "avaliacao-ia");
    /** Pausa entre chamadas, para não estourar o limite de requisições por minuto do Gemini. */
    private static final long PAUSA_MS = Long.parseLong(System.getenv().getOrDefault("AVALIACAO_PAUSA_MS", "5000"));

    @Autowired
    private BuscaGuiasService buscaGuias;

    @Autowired
    private RoteiroIaService roteiroIa;

    @Autowired
    private ChatService chat;

    @Test
    void avaliarRoteirosEChat() throws Exception {
        List<String> linhasMd = new ArrayList<>();
        List<String> linhasCsv = new ArrayList<>();
        StringBuilder respostas = new StringBuilder("# Respostas completas da avaliação\n\nGerado em "
                + OffsetDateTime.now() + "\n");
        linhasCsv.add("caso;tipo;descricao;tempo_ms;fontes;sem_diagnostico;refugio;samu_192;texto_puro;tratamento;recusa;no_tempo;erro");

        linhasMd.add("## Roteiros de contato\n");
        linhasMd.add("| Caso | Perfil | Tempo (s) | Fontes | Sem diagnóstico | Refúgio | 192 | Texto puro | Tratamento | ≤ 15 s |");
        linhasMd.add("|---|---|---|---|---|---|---|---|---|---|");
        for (CasosDeAvaliacao.CasoRoteiro caso : CasosDeAvaliacao.roteiros()) {
            long inicio = System.nanoTime();
            String erro = "";
            RoteiroIa roteiro = null;
            try {
                List<Document> trechos = buscaGuias.buscar(caso.perfil());
                roteiro = roteiroIa.gerar(caso.perfil(), trechos);
            } catch (RuntimeException e) {
                erro = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            long ms = (System.nanoTime() - inicio) / 1_000_000;

            List<String> itens = roteiro == null ? List.of()
                    : Stream.concat(roteiro.roteiro().stream(), roteiro.sinaisAlerta().stream()).toList();
            String texto = String.join("\n", itens);
            boolean ok = roteiro != null;
            boolean fontes = ok && Criterios.citaFontes(itens);
            boolean semDiag = ok && Criterios.semDiagnosticoNemPrescricao(texto);
            boolean refugio = ok && Criterios.refugioCorreto(texto, caso.perfil().refugioNome());
            boolean samu = ok && Criterios.cita192(texto);
            boolean puro = ok && Criterios.textoPuro(itens);
            boolean tratamento = ok && Criterios.tratamentoCorreto(texto, caso.perfil().sexo());

            linhasMd.add("| " + caso.id() + " | " + caso.descricao() + " | " + segundos(ms) + " | " + marca(fontes)
                    + " | " + marca(semDiag) + " | " + marca(refugio) + " | " + marca(samu) + " | " + marca(puro)
                    + " | " + marca(tratamento) + " | " + marca(Criterios.noTempo(ms)) + " |");
            linhasCsv.add(String.join(";", caso.id(), "roteiro", caso.descricao(), String.valueOf(ms), b(fontes),
                    b(semDiag), b(refugio), b(samu), b(puro), b(tratamento), "", b(Criterios.noTempo(ms)), erro));
            respostas.append("\n## ").append(caso.id()).append(" — ").append(caso.descricao()).append("\n\n");
            if (!erro.isEmpty()) respostas.append("**Erro:** ").append(erro).append("\n");
            else {
                respostas.append("**Roteiro**\n\n");
                roteiro.roteiro().forEach(p -> respostas.append("1. ").append(p).append("\n"));
                respostas.append("\n**Sinais de alerta**\n\n");
                roteiro.sinaisAlerta().forEach(p -> respostas.append("- ").append(p).append("\n"));
            }
            Thread.sleep(PAUSA_MS);
        }

        linhasMd.add("\n## Perguntas ao chat\n");
        linhasMd.add("| Caso | Pergunta | Esperado | Tempo (s) | Fonte | Sem diagnóstico | Recusa | 192 | ≤ 15 s |");
        linhasMd.add("|---|---|---|---|---|---|---|---|---|");
        for (CasosDeAvaliacao.CasoChat caso : CasosDeAvaliacao.perguntas()) {
            long inicio = System.nanoTime();
            String resposta;
            String erro = "";
            try {
                resposta = chat.chat(caso.conversa(), caso.pergunta());
            } catch (RuntimeException e) {
                resposta = "";
                erro = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            boolean ok = erro.isEmpty();
            // pergunta fora do tema não precisa citar fonte; as outras sim
            boolean fonte = ok && (caso.deveRecusar() || Criterios.citaFonte(resposta));
            boolean semDiag = ok && Criterios.semDiagnosticoNemPrescricao(resposta);
            boolean recusa = ok && Criterios.recusou(resposta); // só conta nos casos que devem recusar
            boolean samu = ok && (!caso.deveCitar192() || Criterios.cita192(resposta));

            linhasMd.add("| " + caso.id() + " | " + caso.pergunta() + " | " + caso.esperado() + " | " + segundos(ms)
                    + " | " + marca(fonte) + " | " + marca(semDiag) + " | " + (caso.deveRecusar() ? marca(recusa) : "—")
                    + " | " + (caso.deveCitar192() ? marca(samu) : "—") + " | " + marca(Criterios.noTempo(ms)) + " |");
            linhasCsv.add(String.join(";", caso.id(), "chat", caso.pergunta(), String.valueOf(ms), b(fonte), b(semDiag),
                    "", caso.deveCitar192() ? b(samu) : "", "", "", caso.deveRecusar() ? b(recusa) : "",
                    b(Criterios.noTempo(ms)), erro));
            respostas.append("\n## ").append(caso.id()).append(" — ").append(caso.pergunta()).append("\n\n")
                    .append(erro.isEmpty() ? resposta : "**Erro:** " + erro).append("\n");
            Thread.sleep(PAUSA_MS);
        }

        Files.createDirectories(SAIDA);
        escrever("resultados.md", "# Resultados da avaliação do assistente\n\nGerado em " + OffsetDateTime.now()
                + ". ✅ = critério atendido; ❌ = não atendido (verificação automática por texto; confira em respostas.md).\n\n"
                + String.join("\n", linhasMd) + "\n");
        escrever("resultados.csv", String.join("\n", linhasCsv) + "\n");
        escrever("respostas.md", respostas.toString());
        System.out.println(String.join("\n", linhasMd));
    }

    private static void escrever(String arquivo, String conteudo) throws IOException {
        Files.writeString(SAIDA.resolve(arquivo), conteudo, StandardCharsets.UTF_8);
    }

    private static String marca(boolean ok) {
        return ok ? "✅" : "❌";
    }

    private static String b(boolean ok) {
        return ok ? "sim" : "nao";
    }

    private static String segundos(long ms) {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", ms / 1000.0);
    }
}
