package emprestimojogos.infra;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Utilitários de texto: busca sem acento e ocultação de contato no chat. */
public final class Texto {
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern TELEFONE = Pattern.compile("\\+?\\(?\\d[\\d ().-]{7,}\\d");
    public static final String CONTATO_OCULTO = "[contato oculto]";

    private Texto() {
    }

    public static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    /** Minúsculas e sem acentos: "Catã" -> "cata". */
    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    public static boolean contem(String texto, String termo) {
        return normalizar(texto).contains(normalizar(termo));
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /** RNF01: o chat não expõe e-mails nem telefones antes do registro do empréstimo. */
    public static String mascararContato(String texto) {
        String semEmail = EMAIL.matcher(texto).replaceAll(CONTATO_OCULTO);
        return TELEFONE.matcher(semEmail).replaceAll(CONTATO_OCULTO);
    }
}
