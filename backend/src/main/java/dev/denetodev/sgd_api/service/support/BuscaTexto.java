package dev.denetodev.sgd_api.service.support;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Prepara o texto digitado numa busca: sem diferenciar maiúsculas nem acentos, e sem deixar o usuário
 * usar os curingas do banco (% e _) por acaso. O banco precisa aplicar a mesma normalização na coluna
 * (lower + translate com {@link #ACENTOS} e {@link #SEM_ACENTOS}); assim funciona sem extensão do Postgres.
 */
public final class BuscaTexto {

    public static final int MAXIMO = 80;

    public static final String ACENTOS = "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ";
    public static final String SEM_ACENTOS = "aaaaaeeeeiiiiooooouuuucnaaaaaeeeeiiiiooooouuuucn";

    private BuscaTexto() {
    }

    public static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Padrão para LIKE com escape de barra invertida; nulo quando não há o que buscar. */
    public static String padraoLike(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        String texto = bruto.trim();
        if (texto.length() > MAXIMO) {
            texto = texto.substring(0, MAXIMO);
        }
        String normalizado = semAcento(texto).toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + normalizado + "%";
    }
}
