package br.com.thermocity.assistenteiaapi.exception;

import org.springframework.http.HttpStatus;

public class RequisicaoInvalidaException extends AssistenteException {

    public RequisicaoInvalidaException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}