package emprestimojogos.apoio;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Imprime o resultado dos testes no formato da apresentação:
 *
 * <pre>
 * ▶ ServiceUsuarioTest  UC01 Cadastrar conta / UC02 Fazer login
 *   ✔ UC01: cria e persiste a conta com senha em hash
 *   ...
 * ✔ 12 testes · 12 passaram · 0 falhas
 * </pre>
 *
 * Registrado em META-INF/services/org.junit.platform.launcher.TestExecutionListener.
 */
public class RelatorioSlide implements TestExecutionListener {

    private int testesClasse, passaramClasse, falhasClasse;
    private int testesTotal, passaramTotal, falhasTotal, classes;

    @Override
    public void executionStarted(TestIdentifier id) {
        if (ehClasse(id)) {
            ClassSource fonte = (ClassSource) id.getSource().get();
            String nome = fonte.getJavaClass().getSimpleName();
            String titulo = id.getDisplayName().equals(nome) ? "" : "  " + id.getDisplayName();
            System.out.println();
            System.out.println("▶ " + nome + titulo);
            testesClasse = passaramClasse = falhasClasse = 0;
        }
    }

    @Override
    public void executionSkipped(TestIdentifier id, String motivo) {
        if (id.isTest()) {
            System.out.println("  ○ " + id.getDisplayName() + " (ignorado: " + motivo + ")");
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult resultado) {
        if (id.isTest()) {
            testesClasse++;
            if (resultado.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                passaramClasse++;
                System.out.println("  ✔ " + id.getDisplayName());
            } else {
                falhasClasse++;
                System.out.println("  ✘ " + id.getDisplayName());
                resultado.getThrowable().ifPresent(e -> System.out.println("      ↳ " + e));
            }
        } else if (ehClasse(id)) {
            classes++;
            testesTotal += testesClasse;
            passaramTotal += passaramClasse;
            falhasTotal += falhasClasse;
            System.out.println(resumo(testesClasse, passaramClasse, falhasClasse));
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan plano) {
        if (classes > 1) {
            System.out.println();
            System.out.println(resumo(testesTotal, passaramTotal, falhasTotal) + " · " + classes + " classes");
        }
    }

    private static boolean ehClasse(TestIdentifier id) {
        return id.isContainer() && id.getSource().filter(ClassSource.class::isInstance).isPresent();
    }

    private static String resumo(int testes, int passaram, int falhas) {
        return (falhas == 0 ? "✔ " : "✘ ") + testes + " testes · " + passaram + " passaram · " + falhas + " falhas";
    }
}
