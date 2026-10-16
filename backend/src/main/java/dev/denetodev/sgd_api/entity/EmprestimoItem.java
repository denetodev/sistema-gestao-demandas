package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "emprestimo_item")
public class EmprestimoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emprestimo_id", nullable = false)
    private Emprestimo emprestimo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    @Column(name = "quantidade", nullable = false)
    private int quantidade = 1;

    @Column(name = "devolvido_em")
    private OffsetDateTime devolvidoEm;

    protected EmprestimoItem() {
    }

    public EmprestimoItem(Emprestimo emprestimo, Equipamento equipamento, int quantidade) {
        this.emprestimo = emprestimo;
        this.equipamento = equipamento;
        this.quantidade = quantidade;
    }

    public UUID getId() { return id; }
    public Emprestimo getEmprestimo() { return emprestimo; }
    public Equipamento getEquipamento() { return equipamento; }
    public int getQuantidade() { return quantidade; }
    public OffsetDateTime getDevolvidoEm() { return devolvidoEm; }
    public void setDevolvidoEm(OffsetDateTime devolvidoEm) { this.devolvidoEm = devolvidoEm; }
    public boolean pendente() { return devolvidoEm == null; }
}
