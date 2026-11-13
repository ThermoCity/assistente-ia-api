package br.com.thermocity.assistenteiaapi.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record OrientacaoResponse(
        Long pessoaId,
        LocalDate data,
        int nivelCalor,
        String nivelNome,
        Double icPrevistoC,
        List<String> roteiro,
        List<String> sinaisAlerta,
        RefugioIndicado refugio,
        List<String> fontes,
        boolean geradaSemIa,
        String aviso,
        OffsetDateTime geradoEm
) {}
