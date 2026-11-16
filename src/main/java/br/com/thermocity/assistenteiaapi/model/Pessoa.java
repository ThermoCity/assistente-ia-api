package br.com.thermocity.assistenteiaapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

// Pessoa acompanhada (tabela da base territorial), só para leitura.
// De propósito, NÃO mapeia nome, telefone nem contato de referência: esses dados nem chegam ao assistente (LGPD).
@Entity
@Immutable
@Table(name = "TMC_PESSOA_SINTETICA")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Pessoa {

    @Id
    private Long id;
    private Integer idade;
    private String faixaEtaria;
    private Double lat;
    private Double lon;
    private boolean moraSozinho;
    private boolean mobilidadeReduzida;
    private boolean condicaoCronica;
    private String sexo;
}
