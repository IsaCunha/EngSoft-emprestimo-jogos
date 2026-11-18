package br.ufmg.emprestimojogos.service;

import br.ufmg.emprestimojogos.apoio.Cenario;
import br.ufmg.emprestimojogos.excecao.AcessoNegadoException;
import br.ufmg.emprestimojogos.excecao.RegraNegocioException;
import br.ufmg.emprestimojogos.excecao.ValidacaoException;
import br.ufmg.emprestimojogos.infra.Texto;
import br.ufmg.emprestimojogos.model.Emprestimo;
import br.ufmg.emprestimojogos.model.Jogo;
import br.ufmg.emprestimojogos.model.Mensagem;
import br.ufmg.emprestimojogos.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UC08 Negociar pelo chat")
class ServicoChatTest {
    private Cenario cenario;
    private ServicoChat chat;
    private Usuario ana;
    private Usuario bruno;
    private Emprestimo negociacao;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        chat = cenario.sistema.chat();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
        Jogo catan = cenario.novoJogo(ana, "Catan", 200.0);
        negociacao = cenario.negociacao(bruno, catan);
    }

    @Test
    @DisplayName("Mensagens ficam no histórico, em ordem, para as duas partes")
    void historico() {
        chat.enviarMensagem(bruno.getIdUsuario(), negociacao.getIdConversa(), "Oi! Posso pegar no sábado?");
        chat.enviarMensagem(ana.getIdUsuario(), negociacao.getIdConversa(), "Pode sim, por 7 dias.");
        List<Mensagem> historico = chat.abrirChat(ana.getIdUsuario(), negociacao.getIdConversa());
        assertEquals(2, historico.size());
        assertEquals(bruno.getIdUsuario(), historico.get(0).idRemetente());
        assertEquals(historico, chat.abrirChat(bruno.getIdUsuario(), negociacao.getIdConversa()));
    }

    @Test
    @DisplayName("O destinatário é notificado de nova mensagem")
    void notificaDestinatario() {
        chat.enviarMensagem(bruno.getIdUsuario(), negociacao.getIdConversa(), "Olá");
        assertTrue(cenario.sistema.notificacoes().listar(ana.getIdUsuario()).stream()
                .anyMatch(n -> n.getTexto().contains("nova mensagem")));
    }

    @Test
    @DisplayName("RNF01: contato digitado é ocultado durante a negociação")
    void ocultaContato() {
        Mensagem m = chat.enviarMensagem(bruno.getIdUsuario(), negociacao.getIdConversa(),
                "me chama no bruno@gmail.com");
        assertFalse(m.texto().contains("bruno@gmail.com"));
        assertTrue(m.texto().contains(Texto.CONTATO_OCULTO));
    }

    @Test
    @DisplayName("Após o registro do empréstimo, o contato não é mais ocultado")
    void naoOcultaAposRegistro() {
        cenario.sistema.emprestimos().registrarEmprestimo(ana.getIdUsuario(), negociacao.getIdEmprestimo(),
                bruno.getIdUsuario(), cenario.hoje().plusDays(5));
        Mensagem m = chat.enviarMensagem(bruno.getIdUsuario(), negociacao.getIdConversa(), "bruno@gmail.com");
        assertEquals("bruno@gmail.com", m.texto());
    }

    @Test
    @DisplayName("Quem não participa não lê nem escreve na conversa")
    void naoParticipante() {
        Usuario carla = cenario.novoUsuario("Carla");
        assertThrows(AcessoNegadoException.class, () -> chat.abrirChat(carla.getIdUsuario(), negociacao.getIdConversa()));
        assertThrows(AcessoNegadoException.class,
                () -> chat.enviarMensagem(carla.getIdUsuario(), negociacao.getIdConversa(), "oi"));
    }

    @Test
    @DisplayName("Mensagem vazia é rejeitada")
    void mensagemVaziaRejeitada() {
        assertThrows(ValidacaoException.class, () -> chat.enviarMensagem(bruno.getIdUsuario(),
                negociacao.getIdConversa(), "   "));
    }

    @Test
    @DisplayName("Conversa encerrada não aceita mensagens")
    void conversaEncerrada() {
        chat.encerrarConversa(negociacao.getIdConversa(), "teste");
        assertThrows(RegraNegocioException.class,
                () -> chat.enviarMensagem(bruno.getIdUsuario(), negociacao.getIdConversa(), "oi"));
    }

    @Test
    @DisplayName("Inatividade: avisa quem está em falta após 5 dias e encerra 2 dias depois")
    void inatividade() {
        chat.enviarMensagem(bruno.getIdUsuario(), negociacao.getIdConversa(), "Ainda está disponível?");

        cenario.relogio.avancarDias(5);
        assertTrue(chat.verificarInatividade().isEmpty());
        assertTrue(chat.obterConversa(negociacao.getIdConversa()).getDataAvisoInatividade() == null);

        cenario.relogio.avancarDias(1); // mais de 5 dias sem resposta
        assertTrue(chat.verificarInatividade().isEmpty());
        assertTrue(cenario.sistema.notificacoes().listar(ana.getIdUsuario()).stream()
                .anyMatch(n -> n.getTexto().contains("sem resposta")));

        cenario.relogio.avancarDias(2);
        assertEquals(List.of(negociacao.getIdEmprestimo()), chat.verificarInatividade());
        assertFalse(chat.obterConversa(negociacao.getIdConversa()).isAberta());
    }

    @Test
    @DisplayName("Nova mensagem após o aviso reinicia a contagem de inatividade")
    void mensagemReiniciaContagem() {
        cenario.relogio.avancarDias(6);
        chat.verificarInatividade();
        chat.enviarMensagem(ana.getIdUsuario(), negociacao.getIdConversa(), "Desculpa a demora!");
        cenario.relogio.avancarDias(2);
        assertTrue(chat.verificarInatividade().isEmpty());
        assertTrue(chat.obterConversa(negociacao.getIdConversa()).isAberta());
    }
}
