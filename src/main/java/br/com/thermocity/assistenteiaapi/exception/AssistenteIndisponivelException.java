package br.com.thermocity.assistenteiaapi.exception;

import org.springframework.http.HttpStatus;

public class AssistenteIndisponivelException extends AssistenteException {

    public AssistenteIndisponivelException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "O assistente está indisponível no momento. Tente novamente em instantes.", cause);
    }

    public AssistenteIndisponivelException() {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "O assistente está indisponível no momento. Tente novamente em instantes.");
    }
}