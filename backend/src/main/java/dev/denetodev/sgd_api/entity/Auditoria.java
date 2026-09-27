package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tabela", nullable = false, length = 60)
    private String tabela;

    @Column(name = "registro_id", nullable = false)
    private UUID registroId;

    @Column(name = "campo", nullable = false, length = 60)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "text")
    private String valorAnterior;

    @Column(name = "valor_novo", columnDefinition = "text")
    private String valorNovo;

    @Column(name = "alterado_por")
    private UUID alteradoPor;

    @Column(name = "motivo", columnDefinition = "text")
    private String motivo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Auditoria() {
    }

    public Auditoria(String tabela, UUID registroId, String campo, String valorAnterior, String valorNovo, UUID alteradoPor) {
        this.tabela = tabela;
        this.registroId = registroId;
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.alteradoPor = alteradoPor;
    }

    public UUID getId() { return id; }
    public String getTabela() { return tabela; }
    public UUID getRegistroId() { return registroId; }
    public String getCampo() { return campo; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNovo() { return valorNovo; }
    public UUID getAlteradoPor() { return alteradoPor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}