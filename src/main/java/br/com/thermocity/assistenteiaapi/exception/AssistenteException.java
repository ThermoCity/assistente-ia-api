package br.com.thermocity.assistenteiaapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class AssistenteException extends RuntimeException {

    private final HttpStatus status;

    protected AssistenteException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected AssistenteException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}