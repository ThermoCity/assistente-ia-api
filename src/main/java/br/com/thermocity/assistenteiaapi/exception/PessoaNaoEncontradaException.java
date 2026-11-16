package br.com.thermocity.assistenteiaapi.exception;

import org.springframework.http.HttpStatus;

public class PessoaNaoEncontradaException extends AssistenteException {

    public PessoaNaoEncontradaException(Long pessoaId) {
        super(HttpStatus.NOT_FOUND, "Pessoa " + pessoaId + " não encontrada");
    }
}
