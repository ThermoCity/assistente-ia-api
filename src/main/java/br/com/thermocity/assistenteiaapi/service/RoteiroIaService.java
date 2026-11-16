package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.dto.PerfilContato;
import br.com.thermocity.assistenteiaapi.dto.RoteiroIa;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

// Parte "G" do RAG: o modelo escreve o roteiro a partir do perfil (sem identificação) e dos trechos recuperados
@Service
public class RoteiroIaService {

    private static final String SYSTEM_PROMPT = """
            Você é o assistente de busca ativa do ThermoCity. Você ajuda agentes de saúde de uma UBS
            de Presidente Prudente-SP a fazer um contato por telefone com uma pessoa idosa num dia de calor.

            Fontes:
            - Use somente as informações dos trechos dos guias oficiais enviados na mensagem.
              Cada trecho começa com a referência do guia, no formato [Fonte: ...].
            - Ao final de cada passo ou sinal apoiado num trecho, repita a referência do guia entre colchetes.
            - Não invente quantidades de água, doses, temperaturas, telefones ou serviços que não estejam nos trechos
              ou nos dados da pessoa. O único telefone que você pode citar é o 192 (SAMU).

            Limites:
            - Não faça diagnóstico e não prescreva, suspenda nem altere medicamentos.
            - Diante de sinais graves (pele muito quente, confusão mental, desmaio ou convulsão), a conduta é ligar 192 (SAMU).
              Diante de sinais leves, procurar a UBS.
            - Só oriente oferecer líquidos se a pessoa estiver consciente e orientada.

            O que devolver:
            - roteiro: de 5 a 8 passos curtos, na ordem da ligação: como se apresentar e abordar a pessoa;
              o que perguntar; os cuidados adequados ao nível de calor e às condições da pessoa;
              a indicação do refúgio informado (nome e distância), se houver; e quando acionar a equipe da UBS ou o SAMU.
            - sinaisAlerta: de 3 a 6 sinais para o agente observar na conversa, cada um com a conduta.
            - Quanto maior o nível de calor e mais condições de risco a pessoa tiver, mais firme deve ser a orientação.

            Estilo: português do Brasil, frases curtas e simples, tratando a pessoa por "o senhor" ou "a senhora".
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
        return roteiro;
    }

    static String montarPrompt(PerfilContato perfil, List<Document> trechos) {
        StringBuilder texto = new StringBuilder("Dados da pessoa (sem identificação):\n");
        texto.append("- Idade: ").append(perfil.idade() == null ? "60 anos ou mais" : perfil.idade() + " anos").append("\n");
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
                texto.append(", a ").append(perfil.refugioDistanciaMetros()).append(" metros");
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