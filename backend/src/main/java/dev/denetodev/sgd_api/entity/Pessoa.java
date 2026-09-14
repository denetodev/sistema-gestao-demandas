package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 180)
    private String nome;

    @Column(name = "email", length = 180)
    private String email;

    // FKs obrigatórias no banco, mas ainda sem entity própria — ver decisão do passo 4
    @Column(name = "diretoria_id", nullable = false)
    private UUID diretoriaId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(name = "cargo_id")
    private UUID cargoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPessoa status = StatusPessoa.ATIVO;

    @Column(name = "auth_user_id")
    private UUID authUserId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Pessoa() {
    }

    public Pessoa(String nome, UUID diretoriaId, UUID areaId) {
        this.nome = nome;
        this.diretoriaId = diretoriaId;
        this.areaId = areaId;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getDiretoriaId() {
        return diretoriaId;
    }

    public void setDiretoriaId(UUID diretoriaId) {
        this.diretoriaId = diretoriaId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public void setAreaId(UUID areaId) {
        this.areaId = areaId;
    }

    public UUID getCargoId() {
        return cargoId;
    }

    public void setCargoId(UUID cargoId) {
        this.cargoId = cargoId;
    }

    public StatusPessoa getStatus() {
        return status;
    }

    public void setStatus(StatusPessoa status) {
        this.status = status;
    }

    public UUID getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(UUID authUserId) {
        this.authUserId = authUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pessoa other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}