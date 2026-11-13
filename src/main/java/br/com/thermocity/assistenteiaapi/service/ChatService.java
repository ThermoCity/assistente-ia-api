package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.exception.AssistenteIndisponivelException;
import br.com.thermocity.assistenteiaapi.exception.PerguntaMuitoLongaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Você é o assistente de busca ativa do ThermoCity. Você tira dúvidas de agentes de saúde de uma UBS
            de Presidente Prudente-SP sobre calor extremo e cuidados com a pessoa idosa, inclusive durante
            uma ligação com o idoso.

            Idioma e tom:
            - Responda sempre em português do Brasil, com respostas curtas, objetivas e em linguagem simples.

            Escopo:
            - Converse apenas sobre calor extremo, ondas de calor, cuidados com a saúde do idoso no calor
              e o contato da busca ativa.
            - Se o assunto for outro, recuse com educação e diga que só pode ajudar com esses temas.

            Fontes:
            - Baseie suas respostas nos trechos dos guias oficiais fornecidos no contexto
              (Ministério da Saúde, Secretaria de Saúde do Rio de Janeiro e CDC).
            - Se a resposta não estiver nos trechos, diga que não encontrou essa informação nos guias
              e oriente consultar o enfermeiro ou o médico da equipe. Nunca invente.
            - Não invente quantidades de água, doses, temperaturas ou telefones. O único telefone que você
              pode citar é o 192 (SAMU).

            Limites:
            - Não faça diagnóstico e não prescreva, suspenda nem altere medicamentos.
            - Diante de sinais graves (pele muito quente, confusão mental, desmaio ou convulsão),
              a orientação é ligar 192 (SAMU).
            - Sempre que der uma orientação, lembre que ela não substitui a avaliação da equipe de saúde.
            """;

    private static final int LIMITE_PERGUNTA = 1000;

    private final ChatClient chatClient;

    MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT) // persona e regras do assistente
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), // advisor para logar as mensagens
                        MessageChatMemoryAdvisor.builder(memory).build(), // advisor para manter o histórico de mensagens
                        QuestionAnswerAdvisor.builder(vectorStore).build() // advisor do RAG: busca trechos dos guias oficiais
                )
                .build();
    }

    public String chat(String conversationId, String message) {
        if (message.length() > LIMITE_PERGUNTA) {
            throw new PerguntaMuitoLongaException(LIMITE_PERGUNTA);
        }

        String content;
        try {
            content = chatClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)) // passa o conversationId para o advisor de memória
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new AssistenteIndisponivelException(e);
        }

        if (content == null || content.isBlank()) {
            throw new AssistenteIndisponivelException();
        }
        log.info("Resposta do assistente: {}", content);
        return content;
    }
}
