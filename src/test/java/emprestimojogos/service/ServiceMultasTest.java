package br.ufmg.emprestimojogos.service;

import br.ufmg.emprestimojogos.apoio.Cenario;
import br.ufmg.emprestimojogos.excecao.AcessoNegadoException;
import br.ufmg.emprestimojogos.excecao.RegraNegocioException;
import br.ufmg.emprestimojogos.excecao.ValidacaoException;
import br.ufmg.emprestimojogos.model.Emprestimo;
import br.ufmg.emprestimojogos.model.Jogo;
import br.ufmg.emprestimojogos.model.Multa;
import br.ufmg.emprestimojogos.model.StatusConta;
import br.ufmg.emprestimojogos.model.StatusEmprestimo;
import br.ufmg.emprestimojogos.model.TipoMulta;
import br.ufmg.emprestimojogos.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UC12 Regularizar multa e ressarcimento")
class ServiceMultasTest {
    private Cenario cenario;
    private ServiceMultas service;
    private Usuario ana;
    private Usuario bruno;
    private Jogo catan;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        service = cenario.sistema.multas();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
        catan = cenario.novoJogo(ana, "Catan", 200.0);
    }

    private Multa multaDiaria(Emprestimo e) {
        return service.obterMultaPendente(e.getIdEmprestimo(), TipoMulta.DIARIA).orElseThrow();
    }

    @Test
    @DisplayName("Fluxo normal: devolve, paga, desbloqueia e o dono recebe a diária")
    void fluxoNormal() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(2);
        cenario.devolver(ana, bruno, e);

        List<ResumoPendencia> resumo = service.resumoPendencias(bruno.getIdUsuario());
        assertEquals(1, resumo.size());
        assertEquals("Catan", resumo.get(0).nomeJogo());
        assertEquals(2, resumo.get(0).diasAtraso());
        assertEquals(22.0, resumo.get(0).saldoDevedor(), 0.001);
        assertTrue(resumo.get(0).jogoDevolvido());

        ResultadoPagamentoMulta r = service.pagarMulta(bruno.getIdUsuario(), resumo.get(0).idMulta());
        assertTrue(r.aprovado());
        assertTrue(r.contaDesbloqueada());
        assertEquals(22.0, r.valorPago(), 0.001);
        assertEquals(StatusEmprestimo.CONFIRMACAO_ENVIADA, e.getStatus());
        assertEquals(StatusConta.ATIVA, cenario.recarregar(bruno).getStatusConta());
        assertEquals(20.0, cenario.recarregar(ana).getSaldoCredito(), 0.001); // 22 sem os 10% da plataforma
        assertTrue(service.resumoPendencias(bruno.getIdUsuario()).isEmpty());
    }

    @Test
    @DisplayName("FA02: pagamento recusado mantém pendências e restrições")
    void pagamentoRecusado() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.devolver(ana, bruno, e);
        cenario.gateway.recusarPagamentos();

        ResultadoPagamentoMulta r = service.pagarMulta(bruno.getIdUsuario(), multaDiaria(e).getIdMulta());
        assertFalse(r.aprovado());
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());
        assertEquals(0.0, multaDiaria(e).getValorPago(), 0.001);
        assertEquals(0.0, cenario.recarregar(ana).getSaldoCredito(), 0.001);
    }

    @Test
    @DisplayName("FA03: pagar antes de devolver processa o valor, mas mantém o bloqueio")
    void pagarAntesDeDevolver() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(2);

        ResultadoPagamentoMulta r = service.pagarMulta(bruno.getIdUsuario(), multaDiaria(e).getIdMulta());
        assertTrue(r.aprovado());
        assertFalse(r.contaDesbloqueada());
        assertTrue(r.mensagem().contains("após o proprietário confirmar a devolução"));
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());

        cenario.passarDias(1);
        cenario.devolver(ana, bruno, e);
        Multa multa = multaDiaria(e);
        assertEquals(33.0, multa.getValor(), 0.001);
        assertEquals(11.0, multa.getSaldoDevedor(), 0.001); // só o dia a mais

        assertTrue(service.pagarMulta(bruno.getIdUsuario(), multa.getIdMulta()).contaDesbloqueada());
        assertEquals(List.of(22.0, 11.0), cenario.gateway.cobrancas());
    }

    @Test
    @DisplayName("FA03: se o pagamento antecipado cobre tudo, a devolução já encerra a pendência")
    void pagamentoAntecipadoCobreTudo() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(1);
        service.pagarMulta(bruno.getIdUsuario(), multaDiaria(e).getIdMulta()); // R$ 11
        cenario.devolver(ana, bruno, e); // mesmo dia: multa continua R$ 11

        assertEquals(StatusEmprestimo.CONFIRMACAO_ENVIADA, e.getStatus());
        assertEquals(StatusConta.ATIVA, cenario.recarregar(bruno).getStatusConta());
    }

    @Test
    @DisplayName("FA01: pagamento da reposição ressarce o dono e restaura a conta com limite")
    void ressarcimentoExtravio() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(31);
        Multa reposicao = service.obterMultaPendente(e.getIdEmprestimo(), TipoMulta.REPOSICAO).orElseThrow();

        ResultadoPagamentoMulta r = service.pagarMulta(bruno.getIdUsuario(), reposicao.getIdMulta());
        assertTrue(r.aprovado());
        assertTrue(r.contaDesbloqueada());
        assertTrue(r.mensagem().contains("reputação baixa"));
        assertEquals(StatusEmprestimo.RESSARCIDO, e.getStatus());
        assertEquals(200.0, cenario.recarregar(ana).getSaldoCredito(), 0.001);

        Usuario b = cenario.recarregar(bruno);
        assertEquals(StatusConta.ATIVA, b.getStatusConta());
        assertTrue(b.isReputacaoBaixa());
        assertEquals(1, b.getLimiteEmprestimosSimultaneos());
    }

    @Test
    @DisplayName("FA01: conta restaurada fica limitada a 1 empréstimo simultâneo")
    void limiteAposRessarcimento() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(31);
        service.pagarMulta(bruno.getIdUsuario(),
                service.obterMultaPendente(e.getIdEmprestimo(), TipoMulta.REPOSICAO).orElseThrow().getIdMulta());

        Usuario carla = cenario.novoUsuario("Carla");
        Jogo war = cenario.novoJogo(carla, "War", 100.0);
        Jogo uno = cenario.novoJogo(carla, "Uno", 30.0);
        cenario.emprestimoRetirado(carla, bruno, war, 5);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> cenario.negociacao(bruno, uno));
        assertTrue(ex.getMessage().contains("limite"));
    }

    @Test
    @DisplayName("Quitar uma de duas pendências não desbloqueia a conta")
    void outraPendenciaMantemBloqueio() {
        Jogo war = cenario.novoJogo(ana, "War", 100.0);
        Emprestimo e1 = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        Emprestimo e2 = cenario.emprestimoRetirado(ana, bruno, war, 7);
        cenario.relogio.avancarDias(7);
        cenario.sistema.emprestimos().informarNaoDevolucao(ana.getIdUsuario(), e1.getIdEmprestimo());
        cenario.sistema.emprestimos().informarNaoDevolucao(ana.getIdUsuario(), e2.getIdEmprestimo());
        cenario.devolver(ana, bruno, e1);

        ResultadoPagamentoMulta r = service.pagarMulta(bruno.getIdUsuario(), multaDiaria(e1).getIdMulta());
        assertTrue(r.aprovado());
        assertFalse(r.contaDesbloqueada());
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());
    }

    @Test
    @DisplayName("Não se paga multa de outro usuário nem multa já quitada")
    void validacoesDePagamento() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.devolver(ana, bruno, e);
        int idMulta = multaDiaria(e).getIdMulta();
        assertThrows(AcessoNegadoException.class, () -> service.pagarMulta(ana.getIdUsuario(), idMulta));
        service.pagarMulta(bruno.getIdUsuario(), idMulta);
        assertThrows(RegraNegocioException.class, () -> service.pagarMulta(bruno.getIdUsuario(), idMulta));
    }

    @Test
    @DisplayName("Operações do diagrama: gerarMulta, aplicarMulta e editarMulta")
    void operacoesDoDiagrama() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        Multa multa = service.gerarMulta(bruno.getIdUsuario(), e.getIdEmprestimo(), 50.0);
        assertEquals(TipoMulta.DIARIA, multa.getTipo());
        assertEquals(cenario.hoje().plusDays(30), multa.getDataVencimento());
        assertEquals(StatusConta.ATIVA, cenario.recarregar(bruno).getStatusConta());

        service.aplicarMulta(bruno.getIdUsuario(), multa.getIdMulta());
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());

        Multa alterada = new Multa(e.getIdEmprestimo(), bruno.getIdUsuario(), 80.0, TipoMulta.DIARIA,
                cenario.hoje().plusDays(10));
        service.editarMulta(multa.getIdMulta(), alterada);
        assertEquals(80.0, multa.getValor(), 0.001);
        assertEquals(cenario.hoje().plusDays(10), multa.getDataVencimento());

        assertThrows(ValidacaoException.class,
                () -> service.gerarMulta(bruno.getIdUsuario(), e.getIdEmprestimo(), -1.0));
    }

    @Test
    @DisplayName("editarMulta não aceita valor menor que o já pago")
    void editarAbaixoDoPago() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        Multa multa = multaDiaria(e);
        service.pagarMulta(bruno.getIdUsuario(), multa.getIdMulta()); // FA03: paga R$ 11
        Multa menor = new Multa(e.getIdEmprestimo(), bruno.getIdUsuario(), 5.0, TipoMulta.DIARIA, null);
        assertThrows(ValidacaoException.class, () -> service.editarMulta(multa.getIdMulta(), menor));
    }
}
