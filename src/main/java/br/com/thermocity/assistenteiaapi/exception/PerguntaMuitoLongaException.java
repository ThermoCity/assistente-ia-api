package br.com.thermocity.assistenteiaapi.exception;

import org.springframework.http.HttpStatus;

public class PerguntaMuitoLongaException extends AssistenteException {

    public PerguntaMuitoLongaException(int limite) {
        super(HttpStatus.BAD_REQUEST, "A pergunta deve ter no máximo " + limite + " caracteres");
    }
}