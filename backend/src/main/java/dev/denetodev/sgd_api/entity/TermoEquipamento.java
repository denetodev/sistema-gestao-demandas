package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Texto do termo de responsabilidade, versionado: cada empréstimo guarda a versão que foi aceita. */
@Entity
@Table(name = "termo_equipamento")
public class TermoEquipamento {

    @Id
    @Column(name = "versao")
    private int versao;

    @Column(name = "texto", nullable = false, columnDefinition = "text")
    private String texto;

    @Column(name = "criado_por")
    private UUID criadoPor;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TermoEquipamento() {
    }

    public TermoEquipamento(int versao, String texto, UUID criadoPor) {
        this.versao = versao;
        this.texto = texto;
        this.criadoPor = criadoPor;
    }

    public int getVersao() { return versao; }
    public String getTexto() { return texto; }
    public UUID getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
