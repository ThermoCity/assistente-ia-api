package br.com.thermocity.assistenteiaapi.dto;

import java.util.List;

// Tudo o que o modelo recebe sobre a pessoa.
public record PerfilContato(
        Integer idade,
        boolean moraSozinha,
        boolean mobilidadeReduzida,
        boolean condicaoCronica,
        int nivelCalor,
        Double icPrevistoC,
        String refugioNome,
        Integer refugioDistanciaMetros,
        List<String> motivosPrioridade
) {}
