package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.exception.PessoaForaDaListaException;
import br.com.thermocity.assistenteiaapi.repository.ListaDoDiaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// Nível de calor e motivos da prioridade de uma pessoa, lidos da lista do dia gerada pela API de cadastro
@Service
public class ListaDoDiaService {

    /** A pessoa na lista do dia: nível de calor da lista, posição, motivos da prioridade e UBS responsável. */
    public record PessoaNaLista(int nivelCalor, int posicao, List<String> motivos, String ubsNome) {
    }

    private final ListaDoDiaRepository listas;

    public ListaDoDiaService(ListaDoDiaRepository listas) {
        this.listas = listas;
    }

    /** Lança PessoaForaDaListaException (404) se a pessoa não estiver numa lista aberta na data. */
    public PessoaNaLista daPessoa(Long pessoaId, LocalDate data) {
        return listas.itemDaPessoa(pessoaId, data)
                .map(i -> new PessoaNaLista(i.getNivelCalor(), i.getPosicao(), motivos(i.getMotivos()), i.getUbsNome()))
                .orElseThrow(() -> new PessoaForaDaListaException(pessoaId));
    }

    // A API de cadastro grava os motivos num texto só, separados por " · "
    static List<String> motivos(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split("·")).map(String::trim).filter(m -> !m.isEmpty()).toList();
    }
}