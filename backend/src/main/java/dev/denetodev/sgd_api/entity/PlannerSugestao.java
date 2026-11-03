package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma tarefa do Planner que marcou a etiqueta de uma pessoa e espera a decisão dela: aceitar (a tarefa vira demanda ou
 * a pessoa entra na que já existe) ou recusar. Existe uma por tarefa e por pessoa, e nunca é recriada depois de decidida.
 */
@Entity
@Table(name = "planner_sugestao")
public class PlannerSugestao {

    public enum Status { PENDENTE, ACEITA, RECUSADA }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false, length = 64)
    private String planId;

    @Column(name = "tarefa_id", nullable = false, length = 64)
    private String tarefaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(name = "codigo", length = 40)
    private String codigo;

    @Column(name = "titulo", nullable = false, length = 500)
    private String titulo;

    @Column(name = "bucket", length = 120)
    private String bucket;

    @Column(name = "etiqueta", nullable = false, length = 80)
    private String etiqueta;

    @Column(name = "criada_no_planner")
    private OffsetDateTime criadaNoPlanner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status = Status.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demanda_id")
    private Demanda demanda;

    @Column(name = "decidida_em")
    private OffsetDateTime decididaEm;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PlannerSugestao() {
    }

    public PlannerSugestao(String planId, String tarefaId, Pessoa pessoa, String codigo, String titulo, String bucket,
                           String etiqueta, OffsetDateTime criadaNoPlanner) {
        this.planId = planId;
        this.tarefaId = tarefaId;
        this.pessoa = pessoa;
        this.codigo = codigo;
        this.titulo = titulo;
        this.bucket = bucket;
        this.etiqueta = etiqueta;
        this.criadaNoPlanner = criadaNoPlanner;
    }

    public void aceitar(Demanda demanda, OffsetDateTime quando) {
        this.status = Status.ACEITA;
        this.demanda = demanda;
        this.decididaEm = quando;
    }

    public void recusar(OffsetDateTime quando) {
        this.status = Status.RECUSADA;
        this.decididaEm = quando;
    }

    public UUID getId() { return id; }
    public String getPlanId() { return planId; }
    public String getTarefaId() { return tarefaId; }
    public Pessoa getPessoa() { return pessoa; }
    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getBucket() { return bucket; }
    public String getEtiqueta() { return etiqueta; }
    public OffsetDateTime getCriadaNoPlanner() { return criadaNoPlanner; }
    public Status getStatus() { return status; }
    public Demanda getDemanda() { return demanda; }
    public OffsetDateTime getDecididaEm() { return decididaEm; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
