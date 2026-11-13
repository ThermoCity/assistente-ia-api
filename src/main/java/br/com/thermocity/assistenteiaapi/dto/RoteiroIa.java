package br.com.thermocity.assistenteiaapi.dto;

import java.util.List;

// Saída estruturada pedida ao modelo (convertida com .entity())
public record RoteiroIa(
        List<String> roteiro,
        List<String> sinaisAlerta
) {}
