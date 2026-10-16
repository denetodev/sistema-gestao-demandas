package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emprestimo")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "retirante_nome", nullable = false, length = 160)
    private String retiranteNome;

    @Column(name = "retirante_chave_c", length = 8)
    private String retiranteChaveC;

    @Column(name = "retirante_contato", length = 160)
    private String retiranteContato;

    /** Preenchido quando quem retira tem conta no sistema; funcionário do Banco sem conta fica só com nome e chave. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retirante_pessoa_id")
    private Pessoa retirantePessoa;

    @Column(name = "finalidade", columnDefinition = "text")
    private String finalidade;

    /** Sai do prédio: aparece em destaque para quem autoriza. */
    @Column(name = "externo", nullable = false)
    private boolean externo;

    @Column(name = "local_uso", length = 160)
    private String localUso;

    /** Quando a pessoa vai retirar. O equipamento fica reservado de aqui até a devolução prevista. */
    @Column(name = "retirada_prevista", nullable = false)
    private OffsetDateTime retiradaPrevista = OffsetDateTime.now();

    @Column(name = "prevista_devolucao", nullable = false)
    private OffsetDateTime previstaDevolucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private StatusEmprestimo status = StatusEmprestimo.AGUARDANDO_AUTORIZACAO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demanda_id")
    private Demanda demanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id")
    private Atividade atividade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Pessoa registradoPor;

    @Column(name = "termo_versao", nullable = false)
    private int termoVersao;

    @Column(name = "termo_aceito_em", nullable = false)
    private OffsetDateTime termoAceitoEm = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizado_por")
    private Pessoa autorizadoPor;

    @Column(name = "autorizado_em")
    private OffsetDateTime autorizadoEm;

    @Column(name = "motivo_recusa", columnDefinition = "text")
    private String motivoRecusa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retirado_por")
    private Pessoa retiradoPor;

    @Column(name = "retirado_em")
    private OffsetDateTime retiradoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolvido_por")
    private Pessoa devolvidoPor;

    @Column(name = "devolvido_em")
    private OffsetDateTime devolvidoEm;

    @Column(name = "observacoes_devolucao", columnDefinition = "text")
    private String observacoesDevolucao;

    @OneToMany(mappedBy = "emprestimo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmprestimoItem> itens = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Emprestimo() {
    }

    public Emprestimo(String retiranteNome, OffsetDateTime previstaDevolucao, Pessoa registradoPor, int termoVersao) {
        this.retiranteNome = retiranteNome;
        this.previstaDevolucao = previstaDevolucao;
        this.registradoPor = registradoPor;
        this.termoVersao = termoVersao;
    }

    /** Começa a ocupar na retirada prevista, ou antes, se a pessoa levou o equipamento adiantado. */
    public OffsetDateTime inicioOcupacao() {
        return retiradoEm != null && retiradoEm.isBefore(retiradaPrevista) ? retiradoEm : retiradaPrevista;
    }

    /** Fim da ocupação. Retirado e vencido não tem fim: segue com a pessoa até devolver (Instant.MAX). */
    public java.time.Instant fimOcupacao(OffsetDateTime agora) {
        return status == StatusEmprestimo.RETIRADO && previstaDevolucao.isBefore(agora)
                ? java.time.Instant.MAX : previstaDevolucao.toInstant();
    }

    /** Retirado e já passou da devolução prevista. */
    public boolean atrasado(OffsetDateTime agora) {
        return status == StatusEmprestimo.RETIRADO && previstaDevolucao.isBefore(agora);
    }

    public UUID getId() { return id; }
    public String getRetiranteNome() { return retiranteNome; }
    public void setRetiranteNome(String v) { this.retiranteNome = v; }
    public void setPrevistaDevolucao(OffsetDateTime v) { this.previstaDevolucao = v; }
    public String getRetiranteChaveC() { return retiranteChaveC; }
    public void setRetiranteChaveC(String v) { this.retiranteChaveC = v; }
    public String getRetiranteContato() { return retiranteContato; }
    public void setRetiranteContato(String v) { this.retiranteContato = v; }
    public Pessoa getRetirantePessoa() { return retirantePessoa; }
    public void setRetirantePessoa(Pessoa v) { this.retirantePessoa = v; }
    public String getFinalidade() { return finalidade; }
    public void setFinalidade(String v) { this.finalidade = v; }
    public boolean isExterno() { return externo; }
    public void setExterno(boolean v) { this.externo = v; }
    public String getLocalUso() { return localUso; }
    public void setLocalUso(String v) { this.localUso = v; }
    public OffsetDateTime getRetiradaPrevista() { return retiradaPrevista; }
    public void setRetiradaPrevista(OffsetDateTime v) { this.retiradaPrevista = v; }
    public OffsetDateTime getPrevistaDevolucao() { return previstaDevolucao; }
    public StatusEmprestimo getStatus() { return status; }
    public void setStatus(StatusEmprestimo v) { this.status = v; }
    public Demanda getDemanda() { return demanda; }
    public void setDemanda(Demanda v) { this.demanda = v; }
    public Atividade getAtividade() { return atividade; }
    public void setAtividade(Atividade v) { this.atividade = v; }
    public Pessoa getRegistradoPor() { return registradoPor; }
    public int getTermoVersao() { return termoVersao; }
    public OffsetDateTime getTermoAceitoEm() { return termoAceitoEm; }
    public Pessoa getAutorizadoPor() { return autorizadoPor; }
    public void setAutorizadoPor(Pessoa v) { this.autorizadoPor = v; }
    public OffsetDateTime getAutorizadoEm() { return autorizadoEm; }
    public void setAutorizadoEm(OffsetDateTime v) { this.autorizadoEm = v; }
    public String getMotivoRecusa() { return motivoRecusa; }
    public void setMotivoRecusa(String v) { this.motivoRecusa = v; }
    public Pessoa getRetiradoPor() { return retiradoPor; }
    public void setRetiradoPor(Pessoa v) { this.retiradoPor = v; }
    public OffsetDateTime getRetiradoEm() { return retiradoEm; }
    public void setRetiradoEm(OffsetDateTime v) { this.retiradoEm = v; }
    public Pessoa getDevolvidoPor() { return devolvidoPor; }
    public void setDevolvidoPor(Pessoa v) { this.devolvidoPor = v; }
    public OffsetDateTime getDevolvidoEm() { return devolvidoEm; }
    public void setDevolvidoEm(OffsetDateTime v) { this.devolvidoEm = v; }
    public String getObservacoesDevolucao() { return observacoesDevolucao; }
    public void setObservacoesDevolucao(String v) { this.observacoesDevolucao = v; }
    public List<EmprestimoItem> getItens() { return itens; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Emprestimo other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
