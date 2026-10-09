package dev.denetodev.sgd_api.util;

/** CPF é dado pessoal (LGPD): guardado só com dígitos e nunca devolvido por inteiro pela API. */
public final class Cpf {

    private Cpf() {
    }

    /** Remove pontos e traços. Devolve null para null/vazio; não valida. */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    /** Valida 11 dígitos, rejeita sequências repetidas (111.111.111-11) e confere os dois dígitos verificadores. */
    public static boolean valido(String digitos) {
        if (digitos == null || !digitos.matches("\\d{11}") || digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digito(digitos, 9) == digitos.charAt(9) - '0' && digito(digitos, 10) == digitos.charAt(10) - '0';
    }

    /** Ex.: 12345678909 -> ***.456.789-** (mostra o suficiente para o Gestor reconhecer, não para reaproveitar). */
    public static String mascarar(String digitos) {
        if (digitos == null || digitos.length() != 11) {
            return null;
        }
        return "***." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-**";
    }

    private static int digito(String digitos, int quantos) {
        int soma = 0;
        for (int i = 0; i < quantos; i++) {
            soma += (digitos.charAt(i) - '0') * (quantos + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
