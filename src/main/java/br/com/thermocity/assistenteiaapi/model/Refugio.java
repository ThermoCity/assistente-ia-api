package br.com.thermocity.assistenteiaapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "TMC_SERVICO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Refugio {

    @Id
    private String id;
    private String nome;
    private String tipo;
    private String endereco;
    private Double lat;
    private Double lon;
    private String coordStatus;
    private String revisao;
}
