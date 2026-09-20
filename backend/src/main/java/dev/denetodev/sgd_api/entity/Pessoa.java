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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPessoa status = StatusPessoa.ATIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private PerfilPessoa perfil = PerfilPessoa.PROFISSIONAL;

    @Column(name = "auth_user_id")
    private UUID authUserId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Pessoa() {
    }

    public Pessoa(String nome, Area area) {
        this.nome = nome;
        this.area = area;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public StatusPessoa getStatus() { return status; }
    public void setStatus(StatusPessoa status) { this.status = status; }
    public PerfilPessoa getPerfil() { return perfil; }
    public void setPerfil(PerfilPessoa perfil) { this.perfil = perfil; }
    public UUID getAuthUserId() { return authUserId; }
    public void setAuthUserId(UUID authUserId) { this.authUserId = authUserId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pessoa other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}