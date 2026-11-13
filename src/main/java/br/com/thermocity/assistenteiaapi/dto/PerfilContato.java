package br.com.thermocity.assistenteiaapi.dto;

// Tudo o que o modelo recebe sobre a pessoa. Não tem nome, telefone, endereço nem coordenadas (LGPD).
public record PerfilContato(
        Integer idade,
        boolean moraSozinha,
        boolean mobilidadeReduzida,
        boolean condicaoCronica,
        int nivelCalor,
        Double icPrevistoC,
        String refugioNome,
        Integer refugioDistanciaMetros
) {}
