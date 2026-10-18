package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "pessoa_demanda",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pessoa_id", "demanda_id"})
)
public class PessoaDemanda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demanda_id", nullable = false)
    private Demanda demanda;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 30)
    private PapelPessoaDemanda papel = PapelPessoaDemanda.PARTICIPANTE;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    @Column(name = "data_saida")
    private LocalDate dataSaida;

    @Column(name = "observacao", columnDefinition = "text")
    private String observacao;

    /** Quem colocou a pessoa no time; nulo nas linhas antigas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adicionado_por")
    private Pessoa adicionadoPor;

    @Column(name = "adicionado_em")
    private OffsetDateTime adicionadoEm;

    protected PessoaDemanda() {
    }

    public PessoaDemanda(Pessoa pessoa, Demanda demanda) {
        this.pessoa = pessoa;
        this.demanda = demanda;
        this.dataEntrada = LocalDate.now();
    }

    /** Guarda quem adicionou e quando: é o que alimenta o aviso "te adicionaram na demanda". */
    public void registrarAdicao(Pessoa por, OffsetDateTime quando) {
        this.adicionadoPor = por;
        this.adicionadoEm = quando;
    }

    public Pessoa getAdicionadoPor() {
        return adicionadoPor;
    }

    public OffsetDateTime getAdicionadoEm() {
        return adicionadoEm;
    }

    public UUID getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Demanda getDemanda() {
        return demanda;
    }

    public void setDemanda(Demanda demanda) {
        this.demanda = demanda;
    }

    public PapelPessoaDemanda getPapel() {
        return papel;
    }

    public void setPapel(PapelPessoaDemanda papel) {
        this.papel = papel;
    }

    public LocalDate getDataEntrada() {
        return dataEntrada;
    }

    public LocalDate getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDate dataSaida) {
        this.dataSaida = dataSaida;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PessoaDemanda other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}