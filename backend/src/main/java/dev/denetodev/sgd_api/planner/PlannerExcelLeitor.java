package dev.denetodev.sgd_api.planner;

import dev.denetodev.sgd_api.planner.PlannerFonte.PlanoPlanner;
import dev.denetodev.sgd_api.planner.PlannerFonte.TarefaPlanner;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lê o arquivo que o Planner gera em "Exportar plano para o Excel" (aba "Dados Consolidados"). Só lê: nada do arquivo
 * é guardado, só as sugestões que ele gera. As colunas são achadas pelo título, então a ordem não importa.
 * <p>
 * As etiquetas e as pessoas vêm como texto ("CRIATIVO - Lucas", "Lucas Corsino da Conceicao"), separados por ponto e vírgula.
 */
@Component
public class PlannerExcelLeitor {

    /** Id do plano para o que veio de planilha (o arquivo não traz o id do Planner). */
    public static final String PLANO_DA_PLANILHA = "planilha";

    private static final String ABA = "Dados Consolidados";
    private static final String NAO_PARECE = "Esse arquivo não parece a exportação do Planner";
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PlanoPlanner ler(InputStream arquivo) {
        try (Workbook pasta = WorkbookFactory.create(arquivo)) {
            return ler(pasta);
        } catch (ArquivoInvalido e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Não consegui ler o arquivo. Envie o .xlsx exportado pelo Planner, sem abrir e salvar de novo");
        }
    }

    private PlanoPlanner ler(Workbook pasta) {
        Sheet aba = pasta.getSheet(ABA);
        if (aba == null) {
            throw new ArquivoInvalido(NAO_PARECE + ": falta a aba \"" + ABA + "\"");
        }
        Map<String, Integer> colunas = colunas(aba.getRow(aba.getFirstRowNum()));
        Integer id = colunas.get("identificacao da tarefa");
        Integer nome = colunas.get("nome da tarefa");
        if (id == null || nome == null) {
            throw new ArquivoInvalido(NAO_PARECE + ": faltam as colunas \"Identificação da tarefa\" e \"Nome da tarefa\"");
        }
        Integer categoria = colunas.get("categoria");
        Integer atribuido = colunas.get("atribuido a");
        Integer criado = colunas.get("criado em");
        Integer rotulos = colunas.get("rotulos");

        Map<String, String> etiquetas = new LinkedHashMap<>();
        Map<String, String> buckets = new LinkedHashMap<>();
        List<TarefaPlanner> tarefas = new ArrayList<>();
        for (int i = aba.getFirstRowNum() + 1; i <= aba.getLastRowNum(); i++) {
            Row linha = aba.getRow(i);
            String tarefaId = texto(linha, id);
            String titulo = texto(linha, nome);
            if (tarefaId == null || titulo == null) {
                continue;
            }
            String bucket = texto(linha, categoria);
            if (bucket != null) {
                buckets.put(bucket, bucket);
            }
            List<String> rotulosDaTarefa = lista(texto(linha, rotulos));
            rotulosDaTarefa.forEach(r -> etiquetas.put(r, r));
            // O arquivo não diz quanto foi feito de verdade: quem decide o que está encerrado é o bucket (ver PlannerSyncService).
            tarefas.add(new TarefaPlanner(tarefaId, titulo, bucket, data(linha, criado), 0, rotulosDaTarefa,
                    lista(texto(linha, atribuido))));
        }
        return new PlanoPlanner(PLANO_DA_PLANILHA, etiquetas, buckets, tarefas);
    }

    private static final class ArquivoInvalido extends RuntimeException {
        ArquivoInvalido(String mensagem) {
            super(mensagem);
        }
    }

    private static Map<String, Integer> colunas(Row cabecalho) {
        Map<String, Integer> r = new HashMap<>();
        if (cabecalho == null) {
            return r;
        }
        for (Cell c : cabecalho) {
            if (c.getCellType() == CellType.STRING) {
                r.put(simplificar(c.getStringCellValue()), c.getColumnIndex());
            }
        }
        return r;
    }

    private static String texto(Row linha, Integer coluna) {
        if (linha == null || coluna == null) {
            return null;
        }
        Cell c = linha.getCell(coluna);
        if (c == null) {
            return null;
        }
        String v = switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> String.valueOf(c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> null;
        };
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static List<String> lista(String valor) {
        if (valor == null) {
            return List.of();
        }
        return Arrays.stream(valor.split(";")).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    /** O Planner grava só o dia ("2026-10-29"), sem hora; vale o começo do dia. Aceita também data de verdade e dd/MM/aaaa. */
    private static OffsetDateTime data(Row linha, Integer coluna) {
        if (linha == null || coluna == null) {
            return null;
        }
        Cell c = linha.getCell(coluna);
        if (c == null) {
            return null;
        }
        try {
            if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                return c.getLocalDateTimeCellValue().toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            String t = texto(linha, coluna);
            if (t == null) {
                return null;
            }
            String dia = t.length() >= 10 ? t.substring(0, 10) : t;
            LocalDate d = dia.contains("/") ? LocalDate.parse(dia, DATA_BR) : LocalDate.parse(dia);
            return d.atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String simplificar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
}
