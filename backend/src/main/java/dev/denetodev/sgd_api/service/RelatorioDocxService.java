package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemAtividade;
import dev.denetodev.sgd_api.entity.StatusRelatorio;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Gera o DOCX do relatório mensal, para a pessoa assinar e enviar à plataforma da empresa.
 * O conteúdo espelha o documento da tela: atividades agrupadas por demanda, peças do mês,
 * valor de referência, observações e linha de assinatura.
 */
@Service
public class RelatorioDocxService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String AVULSAS = "Atividades avulsas (sem demanda)";
    private static final String FONTE = "Calibri";

    public byte[] gerar(RelatorioResponse relatorio) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            titulo(doc, "RELATÓRIO MENSAL DE ATIVIDADES");
            paragrafo(doc, rotuloMes(relatorio.mes()), ParagraphAlignment.CENTER, false, false, 11, 12);

            if (relatorio.status() != StatusRelatorio.APROVADO) {
                paragrafo(doc, "Rascunho: este relatório ainda não foi aprovado.", ParagraphAlignment.CENTER, false, true, 9, 12);
            }

            identificacao(doc, relatorio);
            atividades(doc, relatorio);
            pecas(doc, relatorio);

            subtitulo(doc, "Observações gerais");
            String obs = relatorio.observacoes();
            paragrafo(doc, obs == null || obs.isBlank() ? "Sem observações." : obs,
                    ParagraphAlignment.LEFT, false, false, 10, 4);

            assinatura(doc, relatorio);

            doc.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o DOCX do relatório", e);
        }
    }

    // ---- blocos ----

    private void identificacao(XWPFDocument doc, RelatorioResponse r) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(120);
        run(p, "Profissional: ", true, false, 10);
        run(p, r.pessoaNome(), false, false, 10);
        p.createRun().addBreak();
        run(p, "Diretoria: ", true, false, 10);
        run(p, orTraco(r.diretoria()) + "    ", false, false, 10);
        run(p, "Área: ", true, false, 10);
        run(p, orTraco(r.area()) + "    ", false, false, 10);
        run(p, "Cargo: ", true, false, 10);
        run(p, orTraco(r.cargo()), false, false, 10);
    }

    private void atividades(XWPFDocument doc, RelatorioResponse r) {
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
            subtitulo(doc, titulo);
            List<String[]> linhas = itens.stream()
                    .map(a -> new String[]{a.data().format(DATA), a.tipo(), orTraco(a.descricao())})
                    .toList();
            tabela(doc, new String[]{"Data", "Tipo", "Descrição"}, new int[]{1500, 2600, 5000}, linhas);
        });
        paragrafoRotulo(doc, "Total: ", incluidas.size() + " atividade(s)");
    }

    private void pecas(XWPFDocument doc, RelatorioResponse r) {
        subtitulo(doc, "Peças produzidas no mês");
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
                new int[]{2300, 2300, 2300, 600, 1400}, linhas);
        BigDecimal total = r.valorReferencia() != null ? r.valorReferencia() : BigDecimal.ZERO;
        paragrafoRotulo(doc, "Valor de referência gerado: ", moeda.format(total));
    }

    private void assinatura(XWPFDocument doc, RelatorioResponse r) {
        XWPFParagraph espaco = doc.createParagraph();
        espaco.setSpacingBefore(900);
        run(espaco, "______________________________________", false, false, 10);

        XWPFParagraph nome = doc.createParagraph();
        run(nome, r.pessoaNome(), false, false, 10);
        if (r.aprovadoEm() != null) {
            XWPFParagraph aprov = doc.createParagraph();
            run(aprov, "aprovado em " + r.aprovadoEm().toLocalDate().format(DATA), false, true, 9);
        }
    }

    // ---- primitivos ----

    private void titulo(XWPFDocument doc, String texto) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        run(p, texto, true, false, 14);
    }

    private void subtitulo(XWPFDocument doc, String texto) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(240);
        p.setSpacingAfter(80);
        p.setKeepNext(true);
        run(p, texto, true, false, 11);
    }

    private void paragrafo(XWPFDocument doc, String texto, ParagraphAlignment alinhamento,
                           boolean negrito, boolean italico, int tamanho, int espacoDepoisPt) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alinhamento);
        p.setSpacingAfter(espacoDepoisPt * 20);
        run(p, texto, negrito, italico, tamanho);
    }

    private void paragrafoRotulo(XWPFDocument doc, String rotulo, String valor) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        run(p, rotulo, true, false, 10);
        run(p, valor, false, false, 10);
    }

    private XWPFRun run(XWPFParagraph p, String texto, boolean negrito, boolean italico, int tamanho) {
        XWPFRun r = p.createRun();
        r.setText(texto);
        r.setBold(negrito);
        r.setItalic(italico);
        r.setFontSize(tamanho);
        r.setFontFamily(FONTE);
        return r;
    }

    private void tabela(XWPFDocument doc, String[] cabecalho, int[] larguras, List<String[]> linhas) {
        XWPFTable t = doc.createTable(1 + linhas.size(), cabecalho.length);
        t.setWidth("100%");
        t.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BBBBBB");
        t.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BBBBBB");
        t.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BBBBBB");
        t.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BBBBBB");
        t.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BBBBBB");
        t.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BBBBBB");

        for (int c = 0; c < cabecalho.length; c++) {
            celula(t.getRow(0).getCell(c), cabecalho[c], true, larguras[c], "F1F1F1");
        }
        for (int i = 0; i < linhas.size(); i++) {
            String[] linha = linhas.get(i);
            for (int c = 0; c < linha.length; c++) {
                celula(t.getRow(i + 1).getCell(c), linha[c], false, larguras[c], null);
            }
        }
    }

    private void celula(XWPFTableCell cell, String texto, boolean negrito, int largura, String fundo) {
        cell.setWidth(String.valueOf(largura));
        if (fundo != null) {
            cell.setColor(fundo);
        }
        XWPFParagraph p = cell.getParagraphArray(0);
        if (p == null) {
            p = cell.addParagraph();
        }
        run(p, texto == null ? "" : texto, negrito, false, 9);
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
