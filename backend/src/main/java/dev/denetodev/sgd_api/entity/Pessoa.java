package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
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

    /** Como a pessoa quer ser chamada (ex.: "Carol"). O nome completo continua em {@code nome}. */
    @Column(name = "nome_exibicao", length = 80)
    private String nomeExibicao;

    /** Chave C (matrícula): C ou F + 7 dígitos, maiúscula, única. */
    @Column(name = "chave_c", length = 8)
    private String chaveC;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "foto_url", columnDefinition = "text")
    private String fotoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPessoa status = StatusPessoa.ATIVO;

    @Column(name = "inativado_em")
    private LocalDate inativadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private PerfilPessoa perfil = PerfilPessoa.PROFISSIONAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referencia_area_id")
    private Area referenciaArea;

    /** Papel dentro do módulo de equipamentos; nulo = só consulta (se a equipe tiver acesso). */
    @Enumerated(EnumType.STRING)
    @Column(name = "papel_equipamentos", length = 30)
    private PapelEquipamentos papelEquipamentos;

    @Column(name = "auth_user_id")
    private UUID authUserId;

    @Column(name = "aprovado_em")
    private OffsetDateTime aprovadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private Pessoa aprovadoPor;

    /** Cadastro importado (sem login) que esta conta pediu para assumir pelo chave C. Só a aprovação de Gestor/Admin concretiza. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vinculo_sugerido_id")
    private Pessoa vinculoSugerido;

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
    public String getNomeExibicao() { return nomeExibicao; }
    public void setNomeExibicao(String nomeExibicao) { this.nomeExibicao = nomeExibicao; }

    public String getChaveC() { return chaveC; }
    public void setChaveC(String chaveC) { this.chaveC = chaveC; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }

    public StatusPessoa getStatus() { return status; }
    public void setStatus(StatusPessoa status) {
        if (status == StatusPessoa.INATIVO && this.status != StatusPessoa.INATIVO) {
            this.inativadoEm = LocalDate.now();
        } else if (status == StatusPessoa.ATIVO) {
            this.inativadoEm = null;
        }
        this.status = status;
    }

    public LocalDate getInativadoEm() { return inativadoEm; }

    public PapelEquipamentos getPapelEquipamentos() { return papelEquipamentos; }
    public void setPapelEquipamentos(PapelEquipamentos papelEquipamentos) { this.papelEquipamentos = papelEquipamentos; }

    public PerfilPessoa getPerfil() { return perfil; }
    public void setPerfil(PerfilPessoa perfil) { this.perfil = perfil; }

    public Area getReferenciaArea() { return referenciaArea; }
    public void setReferenciaArea(Area referenciaArea) { this.referenciaArea = referenciaArea; }

    public UUID getAuthUserId() { return authUserId; }
    public void setAuthUserId(UUID authUserId) { this.authUserId = authUserId; }

    public OffsetDateTime getAprovadoEm() { return aprovadoEm; }
    public void setAprovadoEm(OffsetDateTime aprovadoEm) { this.aprovadoEm = aprovadoEm; }

    public Pessoa getAprovadoPor() { return aprovadoPor; }
    public void setAprovadoPor(Pessoa aprovadoPor) { this.aprovadoPor = aprovadoPor; }

    public Pessoa getVinculoSugerido() { return vinculoSugerido; }
    public void setVinculoSugerido(Pessoa vinculoSugerido) { this.vinculoSugerido = vinculoSugerido; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

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