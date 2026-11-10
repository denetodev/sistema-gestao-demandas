package dev.denetodev.sgd_api.entity;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Como o relatório mensal é montado. Cada função entrega o relatório de um jeito: o Designer lista as
 * demandas com o link da arte, o HTML traz um resumo com tabela por semana e a natureza de cada peça.
 * Quem não tem modelo próprio usa o padrão da norma da Aristocrata.
 */
public enum ModeloRelatorio {
    DESIGNER, HTML, PADRAO;

    /** Decide pelo cargo e, na falta dele, pela equipe da pessoa (ex.: "Designer III - Sênior", "Especialista HTML"). */
    public static ModeloRelatorio de(String cargo, String equipe) {
        ModeloRelatorio porCargo = reconhecer(cargo);
        return porCargo != PADRAO ? porCargo : reconhecer(equipe);
    }

    private static ModeloRelatorio reconhecer(String texto) {
        if (texto == null) {
            return PADRAO;
        }
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (t.contains("design")) {
            return DESIGNER;
        }
        if (t.contains("html")) {
            return HTML;
        }
        return PADRAO;
    }
}
