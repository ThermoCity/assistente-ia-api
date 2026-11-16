package br.com.thermocity.assistenteiaapi.controller;

import br.com.thermocity.assistenteiaapi.dto.OrientacaoResponse;
import br.com.thermocity.assistenteiaapi.service.OrientacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listas/hoje")
@RequiredArgsConstructor
public class OrientacaoController {

    private final OrientacaoService service;

    @GetMapping("/{pessoaId}/orientacao")
    public OrientacaoResponse orientacao(@PathVariable Long pessoaId) {
        return service.orientacaoDeHoje(pessoaId);
    }
}