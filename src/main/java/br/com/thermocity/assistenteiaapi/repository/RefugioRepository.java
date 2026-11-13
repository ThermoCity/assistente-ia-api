package br.com.thermocity.assistenteiaapi.repository;

import br.com.thermocity.assistenteiaapi.model.Refugio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefugioRepository extends JpaRepository<Refugio, String> {

    List<Refugio> findByTipoAndCoordStatusAndRevisao(String tipo, String coordStatus, String revisao);
}
