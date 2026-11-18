package emprestimojogos.apoio;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executa os testes na ordem em que aparecem no arquivo-fonte (a mesma da apresentação).
 * O JUnit não garante essa ordem por padrão. Ativado em junit-platform.properties.
 */
public class OrdemDoCodigo implements MethodOrderer {

    @Override
    public void orderMethods(MethodOrdererContext contexto) {
        String fonte = lerFonte(contexto.getTestClass());
        contexto.getMethodDescriptors().sort(Comparator
                .comparingInt((MethodDescriptor m) -> posicao(fonte, m.getMethod().getName()))
                .thenComparing(m -> m.getMethod().getName()));
    }

    private static int posicao(String fonte, String metodo) {
        Matcher m = Pattern.compile("void\\s+" + Pattern.quote(metodo) + "\\s*\\(").matcher(fonte);
        return m.find() ? m.start() : Integer.MAX_VALUE;
    }

    private static String lerFonte(Class<?> classe) {
        Class<?> externa = classe;
        while (externa.getEnclosingClass() != null) {
            externa = externa.getEnclosingClass();
        }
        Path arquivo = Path.of("src", "test", "java", externa.getName().replace('.', '/') + ".java");
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ""; // sem o fonte, cai na ordem alfabética
        }
    }
}
