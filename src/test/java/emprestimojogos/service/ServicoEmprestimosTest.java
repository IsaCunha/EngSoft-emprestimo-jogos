package emprestimojogos.service;

import emprestimojogos.apoio.Cenario;
import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.classes.Conversa;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.StatusEmprestimo;
import emprestimojogos.classes.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UC07 Manifestar interesse / UC09 Registrar empréstimo / UC10 Confirmar devolução")
class ServicoEmprestimosTest {
    private Cenario cenario;
    private ServicoEmprestimos service;
    private Usuario ana;
    private Usuario bruno;
    private Jogo catan;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        service = cenario.sistema.emprestimos();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
        catan = cenario.novoJogo(ana, "Catan", 200.0);
    }

    // ---------------------------------------------------------------- UC07

    @Test
    @DisplayName("UC07: registra o interesse, cria a conversa e notifica o dono")
    void manifestarInteresse() {
        Emprestimo e = service.manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo());
        assertEquals(StatusEmprestimo.EM_NEGOCIACAO, e.getStatus());
        Conversa conversa = cenario.sistema.chat().obterConversa(e.getIdConversa());
        assertEquals(ana.getIdUsuario(), conversa.getIdDono());
        assertEquals(e.getIdEmprestimo(), conversa.getIdEmprestimo());
        assertTrue(cenario.sistema.notificacoes().listar(ana.getIdUsuario()).stream()
                .anyMatch(n -> n.getTexto().contains("Catan")));
    }

    @Test
    @DisplayName("UC07: usuário bloqueado por multa não pode manifestar interesse")
    void interesseUsuarioBloqueado() {
        cenario.sistema.usuarios().bloquear(bruno.getIdUsuario());
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo()));
        assertTrue(ex.getMessage().contains("bloqueado"));
    }

    @Test
    @DisplayName("UC07: jogo emprestado não aceita interesse")
    void interesseJogoIndisponivel() {
        Usuario carla = cenario.novoUsuario("Carla");
        cenario.emprestimoRetirado(ana, bruno, catan, 7);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.manifestaInteresse(carla.getIdUsuario(), catan.getIdJogo()));
        assertTrue(ex.getMessage().contains("emprestado"));
    }

    @Test
    @DisplayName("UC07: dono não manifesta interesse no próprio jogo")
    void interesseNoProprioJogo() {
        assertThrows(RegraNegocioException.class,
                () -> service.manifestaInteresse(ana.getIdUsuario(), catan.getIdJogo()));
    }

    @Test
    @DisplayName("UC07: não permite duas negociações abertas do mesmo usuário para o mesmo jogo")
    void interesseDuplicado() {
        service.manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo());
        assertThrows(RegraNegocioException.class,
                () -> service.manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo()));
    }

    @Test
    @DisplayName("UC08: negociação sem acordo é encerrada sem empréstimo")
    void cancelarNegociacao() {
        Emprestimo e = service.manifestaInteresse(bruno.getIdUsuario(), catan.getIdJogo());
        service.cancelarNegociacao(ana.getIdUsuario(), e.getIdEmprestimo());
        assertEquals(StatusEmprestimo.NEGOCIACAO_CANCELADA, e.getStatus());
        assertFalse(cenario.sistema.chat().obterConversa(e.getIdConversa()).isAberta());
        assertTrue(catan.isDisponivel());
    }

    // ---------------------------------------------------------------- UC09

    @Test
    @DisplayName("UC09: registro marca o jogo indisponível, calcula a diária (5%) e gera código de retirada")
    void registrarEmprestimo() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        LocalDate prazo = cenario.hoje().plusDays(7);
        service.registrarEmprestimo(ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(), prazo);

        assertEquals(StatusEmprestimo.NEGOCIACAO_ACEITA, e.getStatus());
        assertEquals(prazo, e.getPrazo());
        assertEquals(10.0, e.getValorDiaria(), 0.001);
        assertFalse(catan.isDisponivel());
        assertEquals(6, service.obterCodigoRetirada(ana.getIdUsuario(), e.getIdEmprestimo()).length());
    }

    @Test
    @DisplayName("UC09: interessado confirma o recebimento com o código e o empréstimo fica ativo")
    void confirmarRecebimento() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        assertEquals(StatusEmprestimo.AGUARDANDO_DEVOLUCAO, e.getStatus());
        assertEquals(cenario.hoje(), e.getDataInicio());
        assertEquals(70.0, e.custoPrevisto(), 0.001);
        assertNotNull(service.obterCodigoDevolucao(bruno.getIdUsuario(), e.getIdEmprestimo()));
    }

    @Test
    @DisplayName("UC09: código de retirada errado é recusado")
    void codigoRetiradaInvalido() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        service.registrarEmprestimo(ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(),
                cenario.hoje().plusDays(7));
        assertThrows(RegraNegocioException.class,
                () -> service.confirmarRecebimento(bruno.getIdUsuario(), e.getIdEmprestimo(), "XXXXXX"));
        assertEquals(StatusEmprestimo.NEGOCIACAO_ACEITA, e.getStatus());
    }

    @Test
    @DisplayName("UC09: prazo no passado ou hoje é inválido")
    void prazoInvalido() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.registrarEmprestimo(
                ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(), cenario.hoje().minusDays(1)));
        assertTrue(ex.getCampos().contains("prazo"));
        assertThrows(ValidacaoException.class, () -> service.registrarEmprestimo(
                ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(), cenario.hoje()));
    }

    @Test
    @DisplayName("UC09: interessado bloqueado não pode ser registrado")
    void interessadoBloqueado() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        cenario.sistema.usuarios().bloquear(bruno.getIdUsuario());
        assertThrows(RegraNegocioException.class, () -> service.registrarEmprestimo(
                ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(), cenario.hoje().plusDays(3)));
        assertTrue(catan.isDisponivel());
    }

    @Test
    @DisplayName("UC09: só o dono registra, e para o interessado da negociação")
    void apenasDonoRegistra() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        Usuario carla = cenario.novoUsuario("Carla");
        LocalDate prazo = cenario.hoje().plusDays(3);
        assertThrows(AcessoNegadoException.class, () -> service.registrarEmprestimo(
                bruno.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(), prazo));
        assertThrows(ValidacaoException.class, () -> service.registrarEmprestimo(
                ana.getIdUsuario(), e.getIdEmprestimo(), carla.getIdUsuario(), prazo));
    }

    @Test
    @DisplayName("UC09: ao emprestar, as outras negociações do mesmo jogo são canceladas")
    void cancelaOutrasNegociacoes() {
        Usuario carla = cenario.novoUsuario("Carla");
        Emprestimo daCarla = cenario.negociacao(carla, catan);
        cenario.emprestimoRetirado(ana, bruno, catan, 7);
        assertEquals(StatusEmprestimo.NEGOCIACAO_CANCELADA, daCarla.getStatus());
    }

    @Test
    @DisplayName("UC09: cancelar antes da retirada libera o jogo")
    void cancelarAntesDaRetirada() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        service.registrarEmprestimo(ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(),
                cenario.hoje().plusDays(3));
        service.cancelarNegociacao(bruno.getIdUsuario(), e.getIdEmprestimo());
        assertTrue(catan.isDisponivel());
    }

    // ---------------------------------------------------------------- RNF01

    @Test
    @DisplayName("RNF01: contato só é liberado após o registro do empréstimo")
    void contatoLiberadoAposRegistro() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        assertThrows(AcessoNegadoException.class, () -> service.obterContato(bruno.getIdUsuario(), e.getIdEmprestimo()));

        service.registrarEmprestimo(ana.getIdUsuario(), e.getIdEmprestimo(), bruno.getIdUsuario(),
                cenario.hoje().plusDays(3));
        assertEquals("ana@email.com", service.obterContato(bruno.getIdUsuario(), e.getIdEmprestimo()).email());
        assertEquals("bruno@email.com", service.obterContato(ana.getIdUsuario(), e.getIdEmprestimo()).email());

        Usuario carla = cenario.novoUsuario("Carla");
        assertThrows(AcessoNegadoException.class, () -> service.obterContato(carla.getIdUsuario(), e.getIdEmprestimo()));
    }

    // ---------------------------------------------------------------- UC10

    @Test
    @DisplayName("UC10: dono confirma a devolução com o código; jogo volta a ficar disponível")
    void confirmarDevolucao() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        cenario.relogio.avancarDias(7);
        cenario.devolver(ana, bruno, e);
        assertEquals(StatusEmprestimo.CONFIRMACAO_ENVIADA, e.getStatus());
        assertTrue(e.isDevolvido());
        assertTrue(catan.isDisponivel());
    }

    @Test
    @DisplayName("UC10: devolução antecipada também é aceita")
    void devolucaoAntecipada() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        cenario.relogio.avancarDias(2);
        cenario.devolver(ana, bruno, e);
        assertEquals(StatusEmprestimo.CONFIRMACAO_ENVIADA, e.getStatus());
    }

    @Test
    @DisplayName("UC10: código de devolução errado é recusado")
    void codigoDevolucaoInvalido() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        assertThrows(RegraNegocioException.class,
                () -> service.confirmarDevolucao(ana.getIdUsuario(), e.getIdEmprestimo(), "ERRADO"));
        assertEquals(StatusEmprestimo.AGUARDANDO_DEVOLUCAO, e.getStatus());
    }

    @Test
    @DisplayName("UC10: só o dono confirma; o código de devolução é visto só pelo locatário")
    void permissoesDevolucao() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        String codigo = service.obterCodigoDevolucao(bruno.getIdUsuario(), e.getIdEmprestimo());
        assertThrows(AcessoNegadoException.class,
                () -> service.confirmarDevolucao(bruno.getIdUsuario(), e.getIdEmprestimo(), codigo));
        assertThrows(AcessoNegadoException.class,
                () -> service.obterCodigoDevolucao(ana.getIdUsuario(), e.getIdEmprestimo()));
    }

    @Test
    @DisplayName("UC10: não é possível informar não devolução antes do vencimento")
    void naoDevolucaoAntesDoPrazo() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        assertThrows(RegraNegocioException.class,
                () -> service.informarNaoDevolucao(ana.getIdUsuario(), e.getIdEmprestimo()));
    }

    @Test
    @DisplayName("RF11: o e-mail de confirmação vai para o dono com o link da devolução")
    void emailConfirmacao() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        service.solicitarConfirmacaoDevolucao(e.getIdEmprestimo());
        assertEquals(1, cenario.email.para("ana@email.com").size());
        assertTrue(cenario.email.para("ana@email.com").get(0).corpo().contains("/emprestimos/" + e.getIdEmprestimo()));
        assertEquals(cenario.hoje(), e.getDataUltimoEmailConfirmacao());
    }
}
