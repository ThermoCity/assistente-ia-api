package br.com.thermocity.assistenteiaapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "TMC_ORIENTACAO_CONTATO",
        uniqueConstraints = @UniqueConstraint(name = "uk_tmc_orientacao_pessoa_dia", columnNames = {"pessoa_id", "data_lista"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrientacaoContato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long pessoaId;
    private LocalDate dataLista;
    private Integer nivelCalor;
    private Double icPrevistoC;
    @Column(columnDefinition = "text")
    private String roteiro;
    @Column(columnDefinition = "text")
    private String sinaisAlerta;
    @Column(length = 300)
    private String refugioNome;
    @Column(length = 300)
    private String refugioEndereco;
    private Integer refugioDistanciaMetros;
    @Column(columnDefinition = "text")
    private String fontes;
    @Column(length = 60)
    private String modelo;
    private OffsetDateTime geradoEm;
}
