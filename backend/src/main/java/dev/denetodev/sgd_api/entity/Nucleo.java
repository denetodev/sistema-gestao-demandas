package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Núcleo (House-CRM, Multimídia...): fica dentro de uma diretoria e reúne equipes. */
@Entity
@Table(name = "nucleo", uniqueConstraints = @UniqueConstraint(columnNames = {"diretoria_id", "nome"}))
public class Nucleo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diretoria_id", nullable = false)
    private Diretoria diretoria;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Nucleo() {
    }

    public Nucleo(Diretoria diretoria, String nome) {
        this.diretoria = diretoria;
        this.nome = nome;
    }

    public UUID getId() { return id; }
    public Diretoria getDiretoria() { return diretoria; }
    public void setDiretoria(Diretoria diretoria) { this.diretoria = diretoria; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
