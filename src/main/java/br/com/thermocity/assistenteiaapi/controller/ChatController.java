package br.com.thermocity.assistenteiaapi.controller;

import br.com.thermocity.assistenteiaapi.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    public record ChatRequest(@NotBlank(message = "conversationId é obrigatório") String conversationId,
                              @NotBlank(message = "message é obrigatória") String message){}
    public record ChatResponse(String answer){}

    @PostMapping
    public ChatResponse chat(@RequestBody @Valid ChatRequest request) {
        log.info("Pergunta recebida na conversa {}", request.conversationId());
        return new ChatResponse(chatService.chat(request.conversationId(), request.message()));
    }
}