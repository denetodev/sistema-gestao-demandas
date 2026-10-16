package dev.denetodev.sgd_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "emprestimo_foto")
public class EmprestimoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emprestimo_id", nullable = false)
    private Emprestimo emprestimo;

    @Enumerated(EnumType.STRING)
    @Column(name = "momento", nullable = false, length = 10)
    private MomentoFoto momento;

    @Column(name = "arquivo_path", nullable = false, columnDefinition = "text")
    private String arquivoPath;

    @Column(name = "arquivo_mime", nullable = false, length = 40)
    private String arquivoMime;

    @Column(name = "arquivo_tamanho", nullable = false)
    private int arquivoTamanho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Pessoa createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EmprestimoFoto() {
    }

    public EmprestimoFoto(Emprestimo emprestimo, MomentoFoto momento, String arquivoPath, String arquivoMime,
                          int arquivoTamanho, Pessoa createdBy) {
        this.emprestimo = emprestimo;
        this.momento = momento;
        this.arquivoPath = arquivoPath;
        this.arquivoMime = arquivoMime;
        this.arquivoTamanho = arquivoTamanho;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public Emprestimo getEmprestimo() { return emprestimo; }
    public MomentoFoto getMomento() { return momento; }
    public String getArquivoPath() { return arquivoPath; }
    public String getArquivoMime() { return arquivoMime; }
    public int getArquivoTamanho() { return arquivoTamanho; }
    public Pessoa getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
