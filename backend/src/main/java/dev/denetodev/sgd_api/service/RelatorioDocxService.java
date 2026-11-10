package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemAtividade;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemDemanda;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemEvidencia;
import dev.denetodev.sgd_api.entity.ModeloRelatorio;
import dev.denetodev.sgd_api.entity.StatusRelatorio;
import dev.denetodev.sgd_api.entity.TipoEvidencia;
import dev.denetodev.sgd_api.repository.EvidenciaRepository;
import dev.denetodev.sgd_api.service.support.SemanasDoMes;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gera o DOCX do relatório mensal, para a pessoa assinar e enviar à plataforma da empresa.
 * O documento espelha a tela: faixa de título e blocos na cor do modelo (Designer, HTML ou padrão),
 * tabelas coloridas, demandas em caixas e as imagens das evidências embutidas, inteiras.
 */
@Service
public class RelatorioDocxService {

    /** Lê o arquivo de uma evidência (imagem); vazio quando não há arquivo ou o storage não responde. */
    @FunctionalInterface
    interface LeitorDeArquivo {
        Optional<byte[]> ler(UUID evidenciaId);
    }

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final String AVULSAS = "Atividades avulsas (sem demanda)";
    private static final String FONTE = "Calibri";
    private static final String CINZA = "5B6478";
    private static final String BORDA = "D9DDE6";
    /** Maior imagem cabendo na página, em pixels (96 por polegada). Acima disso reduz, na proporção. */
    private static final int LARGURA_MAXIMA_PX = 560;
    private static final int ALTURA_MAXIMA_PX = 600;

    private final LeitorDeArquivo leitor;

    /** Sem acesso a arquivos: as imagens saem descritas em texto (usado nos testes). */
    public RelatorioDocxService() {
        this(id -> Optional.empty());
    }

    @Autowired
    public RelatorioDocxService(EvidenciaRepository evidencias, ArquivoStorage storage) {
        this(id -> evidencias.findById(id)
                .filter(e -> e.getArquivoPath() != null && !e.getArquivoPath().isBlank())
                .map(e -> storage.baixar(e.getArquivoPath())));
    }

    RelatorioDocxService(LeitorDeArquivo leitor) {
        this.leitor = leitor;
    }

    public byte[] gerar(RelatorioResponse r) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            String cor = corDo(r.modelo());
            bandaDeTitulo(doc, r, cor);

            if (r.status() != StatusRelatorio.APROVADO) {
                paragrafo(doc, "Rascunho: este relatório ainda não foi aprovado.", ParagraphAlignment.CENTER, false, true, 9, 8);
            }

            List<ItemAtividade> incluidas = r.atividades().stream().filter(ItemAtividade::incluida).toList();
            List<ItemDemanda> demandas = RelatorioMensalService.soIncluidas(r.demandas(), incluidas);
            if (demandas.isEmpty()) {
                // sem demandas no mês (ou relatório aprovado antes dos modelos): a lista simples de atividades e peças
                identificacao(doc, r, cor, true);
                atividades(doc, r, cor);
                pecas(doc, r, cor);
            } else {
                identificacao(doc, r, cor, false);
                porModelo(doc, r, demandas, incluidas, cor);
            }

            secao(doc, "Observações" + (r.modelo() == ModeloRelatorio.HTML ? " e Pendências" : " gerais"), cor);
            String obs = r.observacoes();
            paragrafo(doc, obs == null || obs.isBlank() ? "Sem observações." : obs, ParagraphAlignment.LEFT, false, false, 10, 4);

            secao(doc, "DECLARAÇÃO", cor);
            paragrafo(doc, "Declaro que as informações acima refletem fielmente as atividades por mim executadas no período informado "
                    + "e que os materiais anexados correspondem às demandas recebidas e entregas realizadas.",
                    ParagraphAlignment.BOTH, false, false, 10, 6);
            // a declaração acompanha a assinatura: não deixa a assinatura sozinha numa página
            doc.getParagraphs().get(doc.getParagraphs().size() - 1).setKeepNext(true);
            assinatura(doc, r);
            rodape(doc, r);

            doc.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o DOCX do relatório", e);
        }
    }

    // ---- cabeçalho ----

    private void bandaDeTitulo(XWPFDocument doc, RelatorioResponse r, String cor) {
        XWPFTable t = doc.createTable(1, 1);
        t.setWidth("100%");
        bordas(t, cor, cor, cor, cor);
        XWPFTableCell c = t.getRow(0).getCell(0);
        c.setColor(cor);
        XWPFParagraph titulo = c.getParagraphArray(0);
        titulo.setAlignment(ParagraphAlignment.CENTER);
        titulo.setSpacingBefore(160);
        run(titulo, "RELATÓRIO MENSAL DE ATIVIDADES", true, false, 16, "FFFFFF");
        XWPFParagraph norma = c.addParagraph();
        norma.setAlignment(ParagraphAlignment.CENTER);
        run(norma, "Norma Interna nº 32/2026 · Aristocrata Tecnologia e Apoio Administrativo", false, true, 9, "FFFFFF");
        XWPFParagraph mes = c.addParagraph();
        mes.setAlignment(ParagraphAlignment.CENTER);
        mes.setSpacingAfter(160);
        run(mes, rotuloMes(r.mes()), true, false, 11, "FFFFFF");
        espaco(doc, 6);
    }

    /** Quadro de identificação, em duas colunas, no tom claro da cor do modelo. */
    private void identificacao(XWPFDocument doc, RelatorioResponse r, String cor, boolean completa) {
        List<String[]> campos = new ArrayList<>();
        if (completa) {
            campos.add(new String[]{"Profissional", r.pessoaNome()});
            campos.add(new String[]{"Cargo", orTraco(r.cargo())});
            campos.add(new String[]{"Diretoria", orTraco(r.diretoria())});
            campos.add(new String[]{"Área", orTraco(r.area())});
        } else {
            campos.add(new String[]{"Nome do Colaborador", r.pessoaNome()});
            campos.add(new String[]{"Cargo (conforme CTPS)", orTraco(r.cargo())});
            campos.add(new String[]{"Mês de Referência", rotuloMes(r.mes())});
            campos.add(new String[]{"Data de Envio", r.aprovadoEm() != null ? r.aprovadoEm().toLocalDate().format(DATA) : "—"});
        }
        XWPFTable t = doc.createTable(2, 2);
        t.setWidth("100%");
        bordas(t, BORDA, BORDA, BORDA, BORDA);
        t.setInsideHBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
        t.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "FFFFFF");
        for (int i = 0; i < campos.size(); i++) {
            XWPFTableCell c = t.getRow(i / 2).getCell(i % 2);
            c.setColor(tom(cor, 0.07));
            XWPFParagraph p = c.getParagraphArray(0);
            p.setSpacingBefore(60);
            p.setSpacingAfter(60);
            run(p, campos.get(i)[0] + ": ", true, false, 9, CINZA);
            run(p, campos.get(i)[1], true, false, 10, null);
        }
        espaco(doc, 4);
    }

    // ---- modelos por função ----

    private void porModelo(XWPFDocument doc, RelatorioResponse r, List<ItemDemanda> demandas,
                           List<ItemAtividade> incluidas, String cor) {
        List<SemanasDoMes.Semana> semanas = SemanasDoMes.de(YearMonth.parse(r.mes()));
        if (r.modelo() == ModeloRelatorio.HTML) {
            resumoExecutivo(doc, r, semanas, demandas, cor);
        }
        for (SemanasDoMes.Semana semana : semanas) {
            List<ItemDemanda> daSemana = demandas.stream()
                    .filter(d -> d.data() != null && SemanasDoMes.daData(semanas, d.data()).equals(semana)).toList();
            List<ItemAtividade> atividadesDaSemana = incluidas.stream()
                    .filter(a -> SemanasDoMes.daData(semanas, a.data()).equals(semana)).toList();
            if (daSemana.isEmpty() && atividadesDaSemana.isEmpty()) {
                continue;
            }
            secao(doc, "SEMANA " + semana.numero() + " – de " + semana.de().format(DIA_MES) + " a " + semana.ate().format(DIA_MES), cor);
            switch (r.modelo() == null ? ModeloRelatorio.PADRAO : r.modelo()) {
                case DESIGNER -> semanaDesigner(doc, daSemana, cor);
                case HTML -> semanaHtml(doc, daSemana, cor);
                default -> semanaPadrao(doc, daSemana, atividadesDaSemana, cor);
            }
        }
    }

    private void semanaDesigner(XWPFDocument doc, List<ItemDemanda> demandas, String cor) {
        rotulo(doc, "Descrição das Atividades Executadas:", cor);
        int pecas = demandas.stream().mapToInt(ItemDemanda::quantidadePecas).sum();
        paragrafo(doc, demandas.size() + " demanda(s) atendida(s) na semana" + (pecas > 0 ? ", com " + pecas + " peça(s) produzida(s)." : "."),
                ParagraphAlignment.LEFT, false, false, 10, 4);
        rotulo(doc, "Demandas do Banco / Ordens de Serviço:", cor);
        int n = 1;
        for (ItemDemanda d : demandas) {
            Alvo caixa = caixaDeDemanda(doc, cor);
            XWPFParagraph p = caixa.par();
            run(p, String.format("Demanda %02d - %s%s", n++, d.codigo() != null ? d.codigo() + " - " : "", d.titulo()), true, false, 11, null);
            if (d.link() != null) linha(caixa, "Link: ", d.link());
            if (d.descricao() != null && !d.descricao().isBlank()) linha(caixa, "Descrição: ", d.descricao());
            if (d.quantidadePecas() > 0) {
                linha(caixa, "Quantidade de peças feitas: ", String.format("%02d", d.quantidadePecas()) + " (" + resumoPecas(d) + ")");
            }
            if (d.demandante() != null) linha(caixa, "Gestor demandante: ", d.demandante());
            if (d.diretoria() != null) linha(caixa, "Área: ", d.diretoria());
            evidencias(caixa, d, cor);
            espaco(doc, 6);
        }
    }

    private void semanaHtml(XWPFDocument doc, List<ItemDemanda> demandas, String cor) {
        rotulo(doc, "Demandas do Banco / Ordens de Serviço:", cor);
        List<String[]> linhas = demandas.stream().map(d -> new String[]{
                orTraco(d.codigo()), d.titulo(), d.data() != null ? d.data().format(DIA_MES) : "—",
                String.valueOf(d.quantidadePecas()), d.naturezas().isEmpty() ? "—" : String.join(", ", d.naturezas())}).toList();
        tabela(doc, new String[]{"Código BC", "Descrição da demanda", "Data", "QTD", "Natureza"},
                new int[]{1300, 4200, 900, 600, 2200}, linhas, cor, false);
        espaco(doc, 4);
        rotulo(doc, "Evidências (obrigatório):", cor);
        for (ItemDemanda d : demandas) {
            if (d.evidencias().isEmpty()) continue;
            Alvo caixa = caixaDeDemanda(doc, cor);
            run(caixa.par(), "Evidência · " + (d.codigo() != null ? d.codigo() + " " : "") + d.titulo(), true, false, 10, null);
            evidencias(caixa, d, cor);
            espaco(doc, 6);
        }
    }

    private void semanaPadrao(XWPFDocument doc, List<ItemDemanda> demandas, List<ItemAtividade> atividades, String cor) {
        rotulo(doc, "Descrição das Atividades Executadas:", cor);
        Alvo livre = Alvo.doDocumento(doc);
        if (atividades.isEmpty()) {
            paragrafo(doc, "—", ParagraphAlignment.LEFT, false, false, 10, 4);
        }
        for (ItemAtividade a : atividades) {
            marcador(livre, a.data().format(DIA_MES) + " · " + a.tipo()
                    + (a.demandaTitulo() != null ? " · " + a.demandaTitulo() : "")
                    + (a.descricao() != null && !a.descricao().isBlank() ? ": " + a.descricao() : ""));
        }
        rotulo(doc, "Demandas do Banco / Ordens de Serviço:", cor);
        for (ItemDemanda d : demandas) {
            marcador(livre, (d.codigo() != null ? d.codigo() + " - " : "") + d.titulo());
            String origem = d.demandante() != null ? d.demandante() : d.diretoria();
            if (origem != null) marcador(livre, "Origem: " + origem + (d.diretoria() != null && d.demandante() != null ? " (" + d.diretoria() + ")" : ""));
        }
        rotulo(doc, "Evidências (obrigatório):", cor);
        for (ItemDemanda d : demandas) {
            if (d.evidencias().isEmpty()) continue;
            Alvo caixa = caixaDeDemanda(doc, cor);
            run(caixa.par(), (d.codigo() != null ? d.codigo() + " " : "") + d.titulo(), true, false, 10, null);
            evidencias(caixa, d, cor);
            espaco(doc, 6);
        }
    }

    private void resumoExecutivo(XWPFDocument doc, RelatorioResponse r, List<SemanasDoMes.Semana> semanas,
                                 List<ItemDemanda> demandas, String cor) {
        secao(doc, "Resumo Executivo", cor);
        int pecas = demandas.stream().mapToInt(ItemDemanda::quantidadePecas).sum();
        paragrafo(doc, "No mês de " + rotuloMes(r.mes()).toLowerCase(PT_BR) + " foram registradas " + demandas.size()
                + " demanda(s), totalizando " + pecas + " peça(s), distribuídas ao longo de " + semanas.size() + " semana(s) de trabalho.",
                ParagraphAlignment.LEFT, false, false, 10, 6);
        List<String[]> linhas = new ArrayList<>();
        for (SemanasDoMes.Semana s : semanas) {
            List<ItemDemanda> daSemana = demandas.stream()
                    .filter(d -> d.data() != null && SemanasDoMes.daData(semanas, d.data()).equals(s)).toList();
            linhas.add(new String[]{"Semana " + s.numero(), s.de().format(DIA_MES) + " a " + s.ate().format(DIA_MES),
                    String.valueOf(daSemana.size()), String.valueOf(daSemana.stream().mapToInt(ItemDemanda::quantidadePecas).sum())});
        }
        linhas.add(new String[]{"TOTAL", rotuloMes(r.mes()), String.valueOf(demandas.size()), String.valueOf(pecas)});
        tabela(doc, new String[]{"Semana", "Período", "Nº de demandas", "QTD de peças"}, new int[]{1800, 3000, 2200, 2200}, linhas, cor, true);
        espaco(doc, 4);
    }

    // ---- evidências ----

    private void evidencias(Alvo alvo, ItemDemanda d, String cor) {
        if (d.evidencias().isEmpty()) {
            XWPFParagraph p = alvo.par();
            run(p, "Nenhuma evidência guardada nesta demanda.", false, true, 9, "9A3412");
            return;
        }
        XWPFParagraph rot = alvo.par();
        rot.setSpacingBefore(80);
        run(rot, "EVIDÊNCIAS", true, false, 8, CINZA);
        for (ItemEvidencia e : d.evidencias()) {
            if (e.tipo() == TipoEvidencia.LINK && e.link() != null) {
                XWPFParagraph p = alvo.par();
                p.setSpacingAfter(20);
                run(p, "Link da arte: ", true, false, 10, null);
                XWPFRun l = run(p, e.link(), false, false, 10, cor);
                l.setUnderline(UnderlinePatterns.SINGLE);
                continue;
            }
            if (e.temArquivo() && tentarImagem(alvo, e)) {
                continue;
            }
            XWPFParagraph p = alvo.par();
            p.setSpacingAfter(20);
            run(p, "Arquivo: ", true, false, 10, null);
            run(p, (e.descricao() != null && !e.descricao().isBlank() ? e.descricao() : "arquivo anexado")
                    + " (" + e.tipo().name().toLowerCase(PT_BR) + ", anexado no SGP)", false, false, 10, null);
        }
    }

    /** Embute a imagem inteira, reduzida só se passar da largura ou da altura da página. */
    private boolean tentarImagem(Alvo alvo, ItemEvidencia e) {
        int tipoImagem = tipoDeImagem(e.arquivoMime());
        if (tipoImagem < 0) {
            return false;
        }
        try {
            byte[] bytes = leitor.ler(e.id()).orElse(null);
            if (bytes == null || bytes.length == 0) {
                return false;
            }
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return false;
            }
            double escala = Math.min(1.0, Math.min((double) LARGURA_MAXIMA_PX / img.getWidth(), (double) ALTURA_MAXIMA_PX / img.getHeight()));
            int largura = Math.max(1, (int) Math.round(img.getWidth() * escala));
            int altura = Math.max(1, (int) Math.round(img.getHeight() * escala));
            XWPFParagraph p = alvo.par();
            p.setSpacingBefore(60);
            p.setSpacingAfter(40);
            p.setKeepNext(e.descricao() != null && !e.descricao().isBlank());
            XWPFRun run = p.createRun();
            run.addPicture(new ByteArrayInputStream(bytes), tipoImagem, "evidencia-" + e.id(),
                    Units.pixelToEMU(largura), Units.pixelToEMU(altura));
            if (e.descricao() != null && !e.descricao().isBlank()) {
                XWPFParagraph legenda = alvo.par();
                legenda.setSpacingAfter(60);
                run(legenda, e.descricao(), false, true, 9, CINZA);
            }
            return true;
        } catch (Exception ex) {
            // imagem ilegível ou storage fora do ar: o relatório sai assim mesmo, com a evidência descrita em texto
            return false;
        }
    }

    private static int tipoDeImagem(String mime) {
        if (mime == null) return -1;
        return switch (mime.toLowerCase(PT_BR)) {
            case "image/png" -> XWPFDocument.PICTURE_TYPE_PNG;
            case "image/jpeg", "image/jpg" -> XWPFDocument.PICTURE_TYPE_JPEG;
            case "image/gif" -> XWPFDocument.PICTURE_TYPE_GIF;
            case "image/bmp" -> XWPFDocument.PICTURE_TYPE_BMP;
            default -> -1;
        };
    }

    private static String resumoPecas(ItemDemanda d) {
        return d.pecas().stream().map(p -> p.quantidade() + " " + p.tipo()).collect(Collectors.joining(" e "));
    }

    // ---- lista simples (sem demandas no mês) ----

    private void atividades(XWPFDocument doc, RelatorioResponse r, String cor) {
        List<ItemAtividade> incluidas = r.atividades().stream().filter(ItemAtividade::incluida).toList();

        Map<String, List<ItemAtividade>> grupos = new TreeMap<>((a, b) -> {
            if (a.equals(AVULSAS) != b.equals(AVULSAS)) return a.equals(AVULSAS) ? 1 : -1;
            return a.compareToIgnoreCase(b);
        });
        for (ItemAtividade a : incluidas) {
            grupos.computeIfAbsent(a.demandaTitulo() != null ? a.demandaTitulo() : AVULSAS, k -> new ArrayList<>()).add(a);
        }

        if (grupos.isEmpty()) {
            paragrafo(doc, "Nenhuma atividade selecionada.", ParagraphAlignment.LEFT, false, true, 10, 6);
        }
        grupos.forEach((titulo, itens) -> {
            rotulo(doc, titulo, cor);
            List<String[]> linhas = itens.stream()
                    .map(a -> new String[]{a.data().format(DATA), a.tipo(), orTraco(a.descricao())})
                    .toList();
            tabela(doc, new String[]{"Data", "Tipo", "Descrição"}, new int[]{1500, 2600, 5000}, linhas, cor, false);
            espaco(doc, 4);
        });
        paragrafoRotulo(doc, "Total: ", incluidas.size() + " atividade(s)");
    }

    private void pecas(XWPFDocument doc, RelatorioResponse r, String cor) {
        secao(doc, "Peças produzidas no mês", cor);
        if (r.pecas().isEmpty()) {
            paragrafo(doc, "Nenhuma peça registrada no mês.", ParagraphAlignment.LEFT, false, true, 10, 6);
            return;
        }
        NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);
        List<String[]> linhas = r.pecas().stream()
                .map(p -> new String[]{p.demandaTitulo(), p.nome(), p.tipo(),
                        String.valueOf(p.quantidade()), moeda.format(p.valorTotal())})
                .toList();
        tabela(doc, new String[]{"Demanda", "Peça", "Tipo", "Qtd", "Valor"},
                new int[]{2300, 2300, 2300, 600, 1400}, linhas, cor, false);
        BigDecimal total = r.valorReferencia() != null ? r.valorReferencia() : BigDecimal.ZERO;
        paragrafoRotulo(doc, "Valor de referência gerado: ", moeda.format(total));
    }

    // ---- fecho ----

    private void assinatura(XWPFDocument doc, RelatorioResponse r) {
        XWPFParagraph espaco = doc.createParagraph();
        espaco.setSpacingBefore(900);
        espaco.setKeepNext(true);
        run(espaco, "______________________________________", false, false, 10, null);

        XWPFParagraph nome = doc.createParagraph();
        nome.setSpacingAfter(20);
        nome.setKeepNext(true);
        run(nome, "Assinatura do Colaborador: ", true, false, 10, null);
        run(nome, r.pessoaNome(), false, false, 10, null);
        if (r.aprovadoEm() != null) {
            XWPFParagraph aprov = doc.createParagraph();
            run(aprov, "aprovado em " + r.aprovadoEm().toLocalDate().format(DATA), false, true, 9, CINZA);
        }
    }

    private void rodape(XWPFDocument doc, RelatorioResponse r) {
        XWPFFooter rodape = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph p = rodape.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        run(p, "Relatório Mensal de Atividades · " + rotuloMes(r.mes()) + " · " + r.pessoaNome(), false, true, 8, CINZA);
    }

    // ---- primitivos ----

    /** Onde escrever: o corpo do documento ou dentro de uma caixa (célula de tabela). */
    private static final class Alvo {
        private final XWPFDocument doc;
        private final XWPFTableCell celula;
        private boolean primeiro = true;

        private Alvo(XWPFDocument doc, XWPFTableCell celula) {
            this.doc = doc;
            this.celula = celula;
        }

        static Alvo doDocumento(XWPFDocument doc) {
            return new Alvo(doc, null);
        }

        static Alvo daCelula(XWPFTableCell celula) {
            return new Alvo(null, celula);
        }

        XWPFParagraph par() {
            if (celula == null) {
                return doc.createParagraph();
            }
            if (primeiro) {
                primeiro = false;
                return celula.getParagraphArray(0);
            }
            return celula.addParagraph();
        }
    }

    /** Caixa de uma demanda: faixa grossa na cor do modelo à esquerda e fundo claro, como na tela. */
    private Alvo caixaDeDemanda(XWPFDocument doc, String cor) {
        XWPFTable t = doc.createTable(1, 1);
        t.setWidth("100%");
        bordas(t, BORDA, BORDA, BORDA, cor);
        t.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 24, 0, cor);
        XWPFTableCell c = t.getRow(0).getCell(0);
        c.setColor("FBFBFD");
        return Alvo.daCelula(c);
    }

    private void bordas(XWPFTable t, String topo, String base, String direita, String esquerda) {
        t.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, topo);
        t.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, base);
        t.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, direita);
        t.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, esquerda);
    }

    /** Título de seção: texto forte na cor do modelo com uma linha embaixo. */
    private void secao(XWPFDocument doc, String texto, String cor) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(280);
        p.setSpacingAfter(100);
        p.setKeepNext(true);
        p.setBorderBottom(Borders.SINGLE);
        CTBorder borda = p.getCTP().getPPr().getPBdr().getBottom();
        borda.setColor(cor);
        borda.setSz(BigInteger.valueOf(12));
        borda.setSpace(BigInteger.valueOf(2));
        run(p, texto, true, false, 12, null);
    }

    /** Rótulo de bloco (Descrição das Atividades Executadas etc.). */
    private void rotulo(XWPFDocument doc, String texto, String cor) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(160);
        p.setSpacingAfter(60);
        p.setKeepNext(true);
        run(p, texto, true, false, 10, cor);
    }

    private void linha(Alvo alvo, String rotulo, String valor) {
        XWPFParagraph p = alvo.par();
        p.setSpacingAfter(20);
        run(p, rotulo, true, false, 10, null);
        run(p, valor, false, false, 10, null);
    }

    private void marcador(Alvo alvo, String texto) {
        XWPFParagraph p = alvo.par();
        p.setIndentationLeft(360);
        p.setSpacingAfter(20);
        run(p, "•  " + texto, false, false, 10, null);
    }

    private void espaco(XWPFDocument doc, int pontos) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        p.setSpacingBefore(0);
        XWPFRun r = p.createRun();
        r.setFontSize(pontos);
    }

    private void paragrafo(XWPFDocument doc, String texto, ParagraphAlignment alinhamento,
                           boolean negrito, boolean italico, int tamanho, int espacoDepoisPt) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alinhamento);
        p.setSpacingAfter(espacoDepoisPt * 20);
        run(p, texto, negrito, italico, tamanho, null);
    }

    private void paragrafoRotulo(XWPFDocument doc, String rotulo, String valor) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        run(p, rotulo, true, false, 10, null);
        run(p, valor, false, false, 10, null);
    }

    private XWPFRun run(XWPFParagraph p, String texto, boolean negrito, boolean italico, int tamanho, String cor) {
        XWPFRun r = p.createRun();
        r.setText(texto);
        r.setBold(negrito);
        r.setItalic(italico);
        r.setFontSize(tamanho);
        r.setFontFamily(FONTE);
        if (cor != null) {
            r.setColor(cor);
        }
        return r;
    }

    /** Tabela com cabeçalho na cor do modelo, linhas alternadas e, se pedido, a última linha como total. */
    private void tabela(XWPFDocument doc, String[] cabecalho, int[] larguras, List<String[]> linhas, String cor, boolean ultimaEhTotal) {
        XWPFTable t = doc.createTable(1 + linhas.size(), cabecalho.length);
        t.setWidth("100%");
        t.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "C9CED9");
        t.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "C9CED9");
        bordas(t, "C9CED9", "C9CED9", "C9CED9", "C9CED9");

        for (int c = 0; c < cabecalho.length; c++) {
            celula(t.getRow(0).getCell(c), cabecalho[c], true, larguras[c], cor, "FFFFFF");
        }
        for (int i = 0; i < linhas.size(); i++) {
            String[] linha = linhas.get(i);
            boolean total = ultimaEhTotal && i == linhas.size() - 1;
            String fundo = total ? tom(cor, 0.16) : (i % 2 == 1 ? "F6F7FA" : null);
            for (int c = 0; c < linha.length; c++) {
                celula(t.getRow(i + 1).getCell(c), linha[c], total, larguras[c], fundo, null);
            }
            t.getRow(i + 1).getCtRow().addNewTrPr().addNewCantSplit();
        }
    }

    private void celula(XWPFTableCell cell, String texto, boolean negrito, int largura, String fundo, String corTexto) {
        cell.setWidth(String.valueOf(largura));
        if (fundo != null) {
            cell.setColor(fundo);
        }
        XWPFParagraph p = cell.getParagraphArray(0);
        if (p == null) {
            p = cell.addParagraph();
        }
        p.setSpacingBefore(30);
        p.setSpacingAfter(30);
        run(p, texto == null ? "" : texto, negrito, false, 9, corTexto);
    }

    // ---- cores e textos ----

    private static String corDo(ModeloRelatorio modelo) {
        if (modelo == ModeloRelatorio.DESIGNER) return "7C3AED";
        if (modelo == ModeloRelatorio.HTML) return "2563EB";
        return "EA580C";
    }

    /** A cor misturada com branco: 0,07 dá um fundo bem claro, 0,16 um realce. */
    private static String tom(String hex, double intensidade) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return String.format("%02X%02X%02X",
                (int) Math.round(255 - (255 - r) * intensidade),
                (int) Math.round(255 - (255 - g) * intensidade),
                (int) Math.round(255 - (255 - b) * intensidade));
    }

    private static String rotuloMes(String mes) {
        YearMonth ym = YearMonth.parse(mes);
        String nome = ym.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " de " + ym.getYear();
    }

    private static String orTraco(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
