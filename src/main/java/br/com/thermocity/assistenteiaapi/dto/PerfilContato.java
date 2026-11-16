package br.com.thermocity.assistenteiaapi.dto;

import java.util.List;

// Tudo o que o modelo recebe sobre a pessoa. O sexo serve só para o tratamento ("o senhor" ou "a senhora");
// ubsNome é a unidade que faz o contato (não identifica a pessoa).
public record PerfilContato(
        Integer idade,
        boolean moraSozinha,
        boolean mobilidadeReduzida,
        boolean condicaoCronica,
        int nivelCalor,
        Double icPrevistoC,
        String refugioNome,
        Integer refugioDistanciaMetros,
        List<String> motivosPrioridade,
        String sexo,
        String ubsNome
) {

    public String tratamento() {
        if ("F".equalsIgnoreCase(sexo)) return "a senhora";
        if ("M".equalsIgnoreCase(sexo)) return "o senhor";
        return "o senhor ou a senhora";
    }
}
