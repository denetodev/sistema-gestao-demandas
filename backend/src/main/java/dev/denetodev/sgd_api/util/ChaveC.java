package dev.denetodev.sgd_api.util;

import java.util.regex.Pattern;

/**
 * Chave C (matrícula): C (contratado) ou F (funcionário do Banco) seguida de 7 dígitos, ex.: C1349864.
 * Guardada sempre em maiúscula. Identifica a pessoa para ligar a conta de login ao cadastro importado.
 */
public final class ChaveC {

    private static final Pattern FORMATO = Pattern.compile("^[CF][0-9]{7}$");

    private ChaveC() {
    }

    /** Tira espaços e põe em maiúscula. Devolve null para null/vazio; não valida. */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim().toUpperCase(java.util.Locale.ROOT);
        return limpo.isEmpty() ? null : limpo;
    }

    /** Espera o valor já normalizado. */
    public static boolean valido(String chave) {
        return chave != null && FORMATO.matcher(chave).matches();
    }
}
