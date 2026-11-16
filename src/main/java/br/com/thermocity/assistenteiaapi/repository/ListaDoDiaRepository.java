package br.com.thermocity.assistenteiaapi.repository;

import br.com.thermocity.assistenteiaapi.model.Pessoa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Leitura (só leitura) da lista priorizada gerada pela API de cadastro nas tabelas tmc_lista_busca_ativa e
 * tmc_lista_item, no mesmo banco. Daqui vêm o nível de calor do dia e os motivos da prioridade de cada pessoa.
 */
public interface ListaDoDiaRepository extends Repository<Pessoa, Long> {

    /** Item da pessoa numa lista aberta da data (se houver mais de uma, a de maior nível). */
    @Query(value = """
            select l.nivel_calor as nivelCalor, i.posicao as posicao, i.motivos as motivos
              from tmc_lista_item i
              join tmc_lista_busca_ativa l on l.id = i.lista_id
             where i.pessoa_id = :pessoaId
               and l.data_lista = :data
               and l.status = 'ABERTA'
             order by l.nivel_calor desc, i.posicao
             limit 1""", nativeQuery = true)
    Optional<ItemDaLista> itemDaPessoa(@Param("pessoaId") Long pessoaId, @Param("data") LocalDate data);

    /** O que a lista diz sobre a pessoa: nível de calor da lista, posição e motivos ("a · b · c"). */
    interface ItemDaLista {
        Integer getNivelCalor();

        Integer getPosicao();

        String getMotivos();
    }
}