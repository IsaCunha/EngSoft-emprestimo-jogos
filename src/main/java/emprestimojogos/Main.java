package emprestimojogos;

import emprestimojogos.excecao.SistemaException;
import emprestimojogos.infra.GatewayPagamentoSimulado;
import emprestimojogos.infra.ProcessadorImagensImageIO;
import emprestimojogos.infra.RelogioAjustavel;
import emprestimojogos.infra.ServicoEmailConsole;
import emprestimojogos.infra.TabelaValorMercado;
import emprestimojogos.infra.ValidadorCpf;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Usuario;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.DadosCadastro;
import emprestimojogos.service.DadosJogo;
import emprestimojogos.service.ResultadoPagamentoMulta;
import emprestimojogos.service.ResumoPendencia;

import java.nio.file.Files;
import java.time.LocalDate;

/** Demonstração de ponta a ponta: empréstimo, atraso, multa e regularização. */
public class Main {
    public static void main(String[] args) throws Exception {
        RelogioAjustavel relogio = RelogioAjustavel.em(LocalDate.of(2026, 11, 16));
        TabelaValorMercado precos = new TabelaValorMercado().registrar("Catan", 300.0);
        SistemaEmprestimoJogos sistema = new SistemaEmprestimoJogos(Repositorios.emMemoria(), relogio,
                new ServicoEmailConsole(), new GatewayPagamentoSimulado(), precos,
                new ProcessadorImagensImageIO(Files.createTempDirectory("imagens-jogos")));

        titulo("UC01/UC02 - cadastro e login");
        Usuario ana = sistema.usuarios().cadastrarConta(new DadosCadastro("Ana Souza",
                ValidadorCpf.completar("529982247"), "ana", "ana@email.com", "senhaSegura1", "31999990000", null));
        Usuario bruno = sistema.usuarios().cadastrarConta(new DadosCadastro("Bruno Lima",
                ValidadorCpf.completar("111444777"), "bruno", "bruno@email.com", "senhaSegura2", "31988880000", null));
        sistema.usuarios().login("ana@email.com", "senhaSegura1");
        System.out.println("Ana e Bruno cadastrados; Ana fez login.");

        titulo("UC03 - Ana cadastra Catan (valor obtido pela API de mercado)");
        Jogo catan = sistema.jogos().cadastrarJogo(ana.getIdUsuario(),
                DadosJogo.semImagem("Catan", "Jogo de estratégia e troca de recursos", 4, 90, "Ótima", null));
        System.out.printf("Valor estimado: R$ %.2f%n", catan.getValorEstimado());

        titulo("UC06 - Bruno pesquisa \"catan\"");
        sistema.jogos().pesquisar("catan", bruno.getIdUsuario())
                .forEach(i -> System.out.println(i.nomeJogo() + " - " + i.situacao() + " (dono: " + i.apelidoDono() + ")"));

        titulo("UC07/UC08 - interesse e chat");
        Emprestimo emp = sistema.emprestimos().manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo());
        sistema.chat().enviarMensagem(bruno.getIdUsuario(), emp.getIdConversa(),
                "Posso pegar por 7 dias? Meu zap é 31 98888-0000");
        sistema.chat().abrirChat(ana.getIdUsuario(), emp.getIdConversa())
                .forEach(m -> System.out.println("Mensagem: " + m.texto()));

        titulo("UC09 - registro e retirada");
        sistema.emprestimos().registrarEmprestimo(ana.getIdUsuario(), emp.getIdEmprestimo(), bruno.getIdUsuario(),
                LocalDate.now(relogio).plusDays(7));
        String codigoRetirada = sistema.emprestimos().obterCodigoRetirada(ana.getIdUsuario(), emp.getIdEmprestimo());
        sistema.emprestimos().confirmarRecebimento(bruno.getIdUsuario(), emp.getIdEmprestimo(), codigoRetirada);
        System.out.printf("Status: %s | diária R$ %.2f | contato liberado: %s%n",
                emp.getStatus().getDescricao(), emp.getValorDiaria(),
                sistema.emprestimos().obterContato(bruno.getIdUsuario(), emp.getIdEmprestimo()).email());

        titulo("RF10/RF11 - o prazo vence e o agendador envia o e-mail ao dono");
        relogio.avancarDias(7);
        sistema.agendador().executar();

        titulo("UC11 - tolerância expira sem confirmação: penalidade");
        relogio.avancarDias(3);
        sistema.agendador().executar();
        System.out.println("Bruno: " + sistema.usuarios().obterUsuario(bruno.getIdUsuario()).getStatusConta());
        try {
            sistema.emprestimos().manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo());
        } catch (SistemaException e) {
            System.out.println("Bruno tenta outro empréstimo: " + e.getMessage());
        }

        titulo("UC10/UC12 - devolução com atraso e pagamento da multa");
        relogio.avancarDias(2);
        String codigoDevolucao = sistema.emprestimos().obterCodigoDevolucao(bruno.getIdUsuario(), emp.getIdEmprestimo());
        sistema.emprestimos().confirmarDevolucao(ana.getIdUsuario(), emp.getIdEmprestimo(), codigoDevolucao);
        for (ResumoPendencia p : sistema.multas().resumoPendencias(bruno.getIdUsuario())) {
            System.out.printf("Pendência: %s, %d dias de atraso, R$ %.2f%n", p.nomeJogo(), p.diasAtraso(), p.saldoDevedor());
            ResultadoPagamentoMulta r = sistema.multas().pagarMulta(bruno.getIdUsuario(), p.idMulta());
            System.out.println("Pagamento: " + r.mensagem());
        }
        System.out.printf("Status final: %s | Bruno: %s | crédito da Ana: R$ %.2f%n",
                emp.getStatus().getDescricao(),
                sistema.usuarios().obterUsuario(bruno.getIdUsuario()).getStatusConta(),
                sistema.usuarios().obterUsuario(ana.getIdUsuario()).getSaldoCredito());
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("== " + texto + " ==");
    }

    private Main() {
    }
}
