package br.com.thermocity.assistenteiaapi.repository;

import br.com.thermocity.assistenteiaapi.model.Refugio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RefugioRepository extends JpaRepository<Refugio, String> {

    List<Refugio> findByTipoAndCoordStatusAndRevisaoIn(String tipo, String coordStatus, Collection<String> revisoes);
}
