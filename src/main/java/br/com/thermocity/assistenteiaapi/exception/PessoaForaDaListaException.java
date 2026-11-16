package br.com.thermocity.assistenteiaapi.exception;

import org.springframework.http.HttpStatus;

public class PessoaForaDaListaException extends AssistenteException {

    public PessoaForaDaListaException(Long pessoaId) {
        super(HttpStatus.NOT_FOUND, "Pessoa " + pessoaId + " não está na lista de busca ativa de hoje");
    }
}