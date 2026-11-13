package br.com.thermocity.assistenteiaapi.repository;

import br.com.thermocity.assistenteiaapi.model.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
}
