package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "relatorio_mensal")
public class RelatorioMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    /** Sempre o primeiro dia do mês. */
    @Column(name = "mes", nullable = false)
    private LocalDate mes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusRelatorio status = StatusRelatorio.ABERTO;

    @Column(name = "observacoes", columnDefinition = "text")
    private String observacoes;

    @Column(name = "atividades_excluidas", nullable = false, columnDefinition = "text")
    private String atividadesExcluidas = "";

    @Column(name = "snapshot_json", columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "aprovado_em")
    private OffsetDateTime aprovadoEm;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected RelatorioMensal() {
    }

    public RelatorioMensal(Pessoa pessoa, LocalDate mes) {
        this.pessoa = pessoa;
        this.mes = mes;
    }

    public UUID getId() { return id; }
    public Pessoa getPessoa() { return pessoa; }
    public LocalDate getMes() { return mes; }
    public StatusRelatorio getStatus() { return status; }
    public void setStatus(StatusRelatorio status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public OffsetDateTime getAprovadoEm() { return aprovadoEm; }
    public void setAprovadoEm(OffsetDateTime aprovadoEm) { this.aprovadoEm = aprovadoEm; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public Set<UUID> getAtividadesExcluidas() {
        if (atividadesExcluidas == null || atividadesExcluidas.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(atividadesExcluidas.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setAtividadesExcluidas(Set<UUID> ids) {
        this.atividadesExcluidas = ids.stream().map(UUID::toString).collect(Collectors.joining(","));
    }
}
