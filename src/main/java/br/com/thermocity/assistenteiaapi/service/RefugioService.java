package br.com.thermocity.assistenteiaapi.service;

import br.com.thermocity.assistenteiaapi.dto.RefugioIndicado;
import br.com.thermocity.assistenteiaapi.model.Refugio;
import br.com.thermocity.assistenteiaapi.repository.RefugioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Refúgio mais próximo da pessoa, em linha reta, entre todos os cadastrados (públicos e potenciais),
// a mesma regra da lista do dia da API de cadastro.
@Service
@RequiredArgsConstructor
public class RefugioService {

    private static final double RAIO_TERRA_M = 6_371_008.8;
    /** Refúgios públicos confirmados ("manter") e os que dependem de parceria ("potencial"). */
    static final List<String> REVISOES = List.of("manter", "potencial");

    private final RefugioRepository repository;

    public Optional<RefugioIndicado> maisProximo(Double lat, Double lon) {
        if (lat == null || lon == null) return Optional.empty();

        return repository.findByTipoAndCoordStatusAndRevisaoIn("REFUGIO", "OK", REVISOES).stream()
                .filter(r -> r.getLat() != null && r.getLon() != null)
                .min(Comparator.comparingDouble(r -> distanciaMetros(lat, lon, r.getLat(), r.getLon())))
                .map(r -> paraIndicado(r, lat, lon));
    }

    private static RefugioIndicado paraIndicado(Refugio r, double lat, double lon) {
        return new RefugioIndicado(r.getNome(), r.getEndereco(),
                (int) Math.round(distanciaMetros(lat, lon, r.getLat(), r.getLon())));
    }

    // Fórmula de haversine
    static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_M * Math.asin(Math.sqrt(a));
    }
}