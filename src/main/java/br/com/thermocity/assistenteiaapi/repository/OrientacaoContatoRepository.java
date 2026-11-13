package br.com.thermocity.assistenteiaapi.repository;

import br.com.thermocity.assistenteiaapi.model.OrientacaoContato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface OrientacaoContatoRepository extends JpaRepository<OrientacaoContato, Long> {

    Optional<OrientacaoContato> findByPessoaIdAndDataLista(Long pessoaId, LocalDate dataLista);
}
