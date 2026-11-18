package br.ufmg.emprestimojogos.agendamento;

import br.ufmg.emprestimojogos.apoio.Cenario;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Agendador de Prazos (RF10-RF13)")
class AgendadorPrazosTest {
    private Cenario cenario;
    private AgendadorPrazos agendador;
    private Usuario ana;
    private Usuario bruno;
    private Jogo catan;
    private Emprestimo emprestimo;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        agendador = cenario.sistema.agendador();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
        catan = cenario.novoJogo(ana, "Catan", 200.0);
        emprestimo = cenario.emprestimoRetirado(ana, bruno, catan, 7);
    }

    private int emailsDeConfirmacao() {
        return (int) cenario.email.para("ana@email.com").stream()
                .filter(m -> m.assunto().startsWith("Confirme a devolução")).count();
    }

    @Test
    @DisplayName("Antes do vencimento nada acontece")
    void antesDoPrazo() {
        cenario.passarDias(6);
        assertEquals(0, emailsDeConfirmacao());
        assertEquals(StatusEmprestimo.AGUARDANDO_DEVOLUCAO, emprestimo.getStatus());
    }

    @Test
    @DisplayName("RF11: no vencimento envia um e-mail ao dono, sem duplicar no mesmo dia")
    void emailNoVencimento() {
        cenario.passarDias(7);
        assertEquals(1, emailsDeConfirmacao());
        agendador.executar();
        assertEquals(1, emailsDeConfirmacao());
    }

    @Test
    @DisplayName("UC10: sem resposta do dono, o e-mail é reenviado diariamente durante a tolerância")
    void reenvioPeriodico() {
        cenario.passarDias(9); // dias 7, 8 e 9
        assertEquals(3, emailsDeConfirmacao());
        assertEquals(StatusEmprestimo.AGUARDANDO_DEVOLUCAO, emprestimo.getStatus());
    }

    @Test
    @DisplayName("UC10: falha no envio é registrada e o envio é tentado de novo")
    void falhaNoEnvio() {
        cenario.relogio.avancarDias(7);
        cenario.email.falharProximos(1);
        agendador.executar();
        assertEquals(0, emailsDeConfirmacao());
        assertEquals(1, agendador.getRegistroErros().size());

        agendador.executar(); // nova tentativa
        assertEquals(1, emailsDeConfirmacao());
    }

    @Test
    @DisplayName("UC11: tolerância expirada sem confirmação aplica a penalidade")
    void toleranciaExpirada() {
        cenario.passarDias(10); // prazo + 3 dias
        assertEquals(StatusEmprestimo.COM_PENDENCIA, emprestimo.getStatus());
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());
        Multa multa = cenario.sistema.multas().obterMultaPendente(emprestimo.getIdEmprestimo(), TipoMulta.DIARIA)
                .orElseThrow();
        assertEquals(33.0, multa.getValor(), 0.001); // 3 dias x R$ 11
    }

    @Test
    @DisplayName("Confirmação dentro da tolerância evita a penalidade")
    void confirmacaoNaTolerancia() {
        cenario.passarDias(8);
        cenario.devolver(ana, bruno, emprestimo);
        cenario.passarDias(5);
        assertEquals(StatusEmprestimo.CONFIRMACAO_ENVIADA, emprestimo.getStatus());
        assertEquals(StatusConta.ATIVA, cenario.recarregar(bruno).getStatusConta());
        assertTrue(cenario.sistema.multas().listarPendentes(bruno.getIdUsuario()).isEmpty());
    }

    @Test
    @DisplayName("RF13: a multa acumula diariamente e, após o limite, o jogo é declarado extraviado")
    void acumuloEExtravio() {
        cenario.passarDias(12);
        assertEquals(55.0, cenario.sistema.multas()
                .obterMultaPendente(emprestimo.getIdEmprestimo(), TipoMulta.DIARIA).orElseThrow().getValor(), 0.001);

        cenario.passarDias(26); // prazo + 31
        assertEquals(StatusEmprestimo.EXTRAVIADO, emprestimo.getStatus());
        assertEquals(StatusConta.SUSPENSA, cenario.recarregar(bruno).getStatusConta());
    }

    @Test
    @DisplayName("Multas não pagas no prazo de 30 dias são marcadas como vencidas")
    void multaVencida() {
        cenario.relogio.avancarDias(7);
        cenario.sistema.emprestimos().informarNaoDevolucao(ana.getIdUsuario(), emprestimo.getIdEmprestimo());
        cenario.devolver(ana, bruno, emprestimo);
        Multa multa = cenario.sistema.multas().obterMultaPendente(emprestimo.getIdEmprestimo(), TipoMulta.DIARIA)
                .orElseThrow();

        cenario.passarDias(30);
        assertEquals(false, multa.isVencida());
        cenario.passarDias(1);
        assertTrue(multa.isVencida());
    }

    @Test
    @DisplayName("UC08: negociação parada é encerrada pelo agendador e o empréstimo é cancelado")
    void inatividadeDoChat() {
        Usuario carla = cenario.novoUsuario("Carla");
        Jogo war = cenario.novoJogo(ana, "War", 100.0);
        Emprestimo negociacao = cenario.negociacao(carla, war);
        cenario.passarDias(8); // 6 dias: aviso; +2 dias: encerramento
        assertEquals(StatusEmprestimo.NEGOCIACAO_CANCELADA, negociacao.getStatus());
    }
}
