package emprestimojogos.infra;

/** Validação de CPF pelos dígitos verificadores. */
public final class ValidadorCpf {
    private ValidadorCpf() {
    }

    public static String somenteDigitos(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return d.equals(completar(d.substring(0, 9)));
    }

    /** Recebe os 9 primeiros dígitos e devolve o CPF completo com os verificadores. */
    public static String completar(String noveDigitos) {
        String base = somenteDigitos(noveDigitos);
        if (base.length() != 9) {
            throw new IllegalArgumentException("Informe 9 dígitos");
        }
        int d1 = digito(base, 10);
        int d2 = digito(base + d1, 11);
        return base + d1 + d2;
    }

    private static int digito(String parcial, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < parcial.length(); i++) {
            soma += (parcial.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
