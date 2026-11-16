package emprestimojogos.infra;

import java.security.SecureRandom;

/** Gera os códigos curtos e aleatórios de retirada (UC09) e de devolução (UC10). */
public final class GeradorCodigo {
    /** Sem caracteres ambíguos (0/O, 1/I). */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int TAMANHO = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeradorCodigo() {
    }

    public static String gerar() {
        StringBuilder sb = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    public static boolean confere(String esperado, String informado) {
        return esperado != null && informado != null && esperado.equalsIgnoreCase(informado.trim());
    }
}
