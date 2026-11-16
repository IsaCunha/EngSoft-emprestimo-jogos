package br.ufmg.emprestimojogos.service;

import br.ufmg.emprestimojogos.apoio.Cenario;
import br.ufmg.emprestimojogos.excecao.RegraNegocioException;
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

@DisplayName("UC11 Aplicar penalidade")
class AplicarPenalidadeTest {
    private Cenario cenario;
    private Usuario ana;
    private Usuario bruno;
    private Jogo catan;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
        catan = cenario.novoJogo(ana, "Catan", 200.0); // diária R$ 10, multa diária R$ 11
    }

    @Test
    @DisplayName("Dono informa não devolução: atraso, multa diária, bloqueio e e-mail ao inadimplente")
    void fluxoNormal() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);

        assertEquals(StatusEmprestimo.COM_PENDENCIA, e.getStatus());
        Multa multa = cenario.sistema.multas().obterMultaPendente(e.getIdEmprestimo(), TipoMulta.DIARIA).orElseThrow();
        assertEquals(11.0, multa.getValor(), 0.001);
        assertEquals(bruno.getIdUsuario(), multa.getIdUsuario());
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());

        var emails = cenario.email.para("bruno@email.com");
        assertEquals(1, emails.size());
        assertTrue(emails.get(0).corpo().contains("R$ 11,00"));
        assertTrue(emails.get(0).corpo().contains(e.getPrazo().plusDays(30).toString()));
    }

    @Test
    @DisplayName("Multa diária acumula a cada dia de atraso")
    void multaAcumula() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(4);
        Multa multa = cenario.sistema.multas().obterMultaPendente(e.getIdEmprestimo(), TipoMulta.DIARIA).orElseThrow();
        assertEquals(44.0, multa.getValor(), 0.001);
    }

    @Test
    @DisplayName("FA02: usuário já bloqueado recebe o novo débito e continua bloqueado")
    void usuarioJaBloqueado() {
        Jogo war = cenario.novoJogo(ana, "War", 100.0);
        Emprestimo e1 = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        Emprestimo e2 = cenario.emprestimoRetirado(ana, bruno, war, 7);
        cenario.relogio.avancarDias(7);
        cenario.sistema.emprestimos().informarNaoDevolucao(ana.getIdUsuario(), e1.getIdEmprestimo());
        cenario.sistema.emprestimos().informarNaoDevolucao(ana.getIdUsuario(), e2.getIdEmprestimo());

        assertEquals(2, cenario.sistema.multas().listarPendentes(bruno.getIdUsuario()).size());
        assertEquals(StatusConta.BLOQUEADA, cenario.recarregar(bruno).getStatusConta());
    }

    @Test
    @DisplayName("FA01: após 30 dias de atraso o jogo é dado como extraviado")
    void extravio() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(30);
        assertEquals(StatusEmprestimo.COM_PENDENCIA, e.getStatus());

        cenario.passarDias(1);
        assertEquals(StatusEmprestimo.EXTRAVIADO, e.getStatus());

        List<Multa> pendentes = cenario.sistema.multas().listarPendentes(bruno.getIdUsuario());
        assertEquals(1, pendentes.size());
        assertEquals(TipoMulta.REPOSICAO, pendentes.get(0).getTipo());
        assertEquals(220.0, pendentes.get(0).getValor(), 0.001); // 200 + 10% administrativa

        Usuario b = cenario.recarregar(bruno);
        assertEquals(StatusConta.SUSPENSA, b.getStatusConta());
        assertTrue(b.isReputacaoBaixa());
        assertEquals(1, cenario.email.para("ana@email.com").stream()
                .filter(m -> m.assunto().contains("extraviado")).count());
        assertEquals(1, cenario.email.para("bruno@email.com").stream()
                .filter(m -> m.assunto().contains("reposição")).count());
    }

    @Test
    @DisplayName("FA01: o valor de diária já pago é mantido como quitado no extravio")
    void extravioComPagamentoParcial() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(2);
        Multa diaria = cenario.sistema.multas().obterMultaPendente(e.getIdEmprestimo(), TipoMulta.DIARIA).orElseThrow();
        cenario.sistema.multas().pagarMulta(bruno.getIdUsuario(), diaria.getIdMulta()); // paga R$ 22 (FA03)

        cenario.passarDias(29);
        assertEquals(StatusEmprestimo.EXTRAVIADO, e.getStatus());
        assertTrue(diaria.isPaga());
        assertEquals(22.0, diaria.getValor(), 0.001);
    }

    @Test
    @DisplayName("Extravio só vale para empréstimo em atraso e não devolvido")
    void extravioInvalido() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        assertThrows(RegraNegocioException.class,
                () -> cenario.sistema.emprestimos().declararExtravio(e.getIdEmprestimo()));
    }

    @Test
    @DisplayName("Falha no e-mail não impede a penalidade e o erro é registrado")
    void falhaNoEmail() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        cenario.relogio.avancarDias(7);
        cenario.email.falharProximos(1);
        cenario.sistema.emprestimos().informarNaoDevolucao(ana.getIdUsuario(), e.getIdEmprestimo());

        assertEquals(StatusEmprestimo.COM_PENDENCIA, e.getStatus());
        assertEquals(1, cenario.sistema.emprestimos().getErrosEnvioEmail().size());
    }

    @Test
    @DisplayName("Devolução com atraso fecha a multa no dia da devolução")
    void devolucaoComAtraso() {
        Emprestimo e = cenario.emprestimoAtrasado(ana, bruno, catan);
        cenario.passarDias(3);
        cenario.devolver(ana, bruno, e);
        cenario.passarDias(5);

        assertTrue(e.isDevolvido());
        assertTrue(catan.isDisponivel());
        Multa multa = cenario.sistema.multas().obterMultaPendente(e.getIdEmprestimo(), TipoMulta.DIARIA).orElseThrow();
        assertEquals(33.0, multa.getValor(), 0.001);
        assertEquals(StatusEmprestimo.COM_PENDENCIA, e.getStatus()); // falta pagar
        assertFalse(cenario.recarregar(bruno).getStatusConta() == StatusConta.ATIVA);
    }
}
