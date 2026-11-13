package br.com.thermocity.assistenteiaapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Você é o assistente de busca ativa do ThermoCity. Você ajuda agentes de saúde de uma UBS
            de Presidente Prudente-SP a fazer um contato por telefone com uma pessoa idosa num dia de calor.

            Fontes:
            - Use somente as informações dos trechos dos guias oficiais enviados na mensagem.
            - Ao final de cada passo ou sinal apoiado num trecho, indique a fonte entre colchetes, por exemplo [Fonte 2].
            - Não invente quantidades de água, doses, temperaturas, telefones ou serviços que não estejam nos trechos
              ou nos dados da pessoa. O único telefone que você pode citar é o 192 (SAMU).

            Limites:
            - Não faça diagnóstico e não prescreva nem altere medicamentos.
            - Diante de sinais graves, a conduta é ligar 192 (SAMU). Diante de sinais leves, procurar a UBS.

            O que devolver:
            - roteiro: de 5 a 8 passos curtos, na ordem da ligação: como se apresentar e abordar a pessoa;
              o que perguntar; os cuidados adequados ao nível de calor e às condições da pessoa;
              a indicação do refúgio informado (nome e distância), se houver; e quando acionar a equipe da UBS ou o SAMU.
            - sinaisAlerta: de 3 a 6 sinais para o agente observar na conversa, cada um com a conduta.
            - Quanto maior o nível de calor e mais condições de risco a pessoa tiver, mais firme deve ser a orientação.

            Estilo: português do Brasil, frases curtas e simples, tratando a pessoa por "o senhor" ou "a senhora".
            """;

    private final ChatClient chatClient;

    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT) //persona e regras da NutriBot
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), //advisor para logar as mensagens
                        MessageChatMemoryAdvisor.builder(memory).build() //advisor para manter o histórico de mensagens
                        //QuestionAnswerAdvisor.builder(vectorStore).build() //advisor do RAG: busca trechos dos documentos da clínica
                )
                .build();
    }

    public String chat(String conversationId, String message) {
        System.out.println("Conversation ID: " + conversationId);
        String content = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)) // passa o conversationId para o advisor de memória
                .call()
                .content();
        log.info("Chat response: {}", content);
        return content;
    }
}
