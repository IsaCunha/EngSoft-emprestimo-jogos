package emprestimojogos.infra;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash de senhas com PBKDF2 + salt (RNF01: senhas nunca são armazenadas em texto puro). */
public final class HashSenha {
    private static final int ITERACOES = 20_000;
    private static final int TAMANHO_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private HashSenha() {
    }

    public static String gerar(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return codificar(salt) + ":" + codificar(pbkdf2(senha, salt));
    }

    public static boolean verificar(String senha, String armazenado) {
        if (senha == null || armazenado == null || !armazenado.contains(":")) {
            return false;
        }
        String[] partes = armazenado.split(":");
        byte[] salt = Base64.getDecoder().decode(partes[0]);
        byte[] esperado = Base64.getDecoder().decode(partes[1]);
        return MessageDigest.isEqual(esperado, pbkdf2(senha, salt));
    }

    private static byte[] pbkdf2(String senha, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, ITERACOES, TAMANHO_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
