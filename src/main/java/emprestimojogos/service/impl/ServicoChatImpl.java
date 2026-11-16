package emprestimojogos.service.impl;

import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.infra.Texto;
import emprestimojogos.classes.Conversa;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Mensagem;
import emprestimojogos.classes.RegrasNegocio;
import emprestimojogos.classes.Usuario;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.ServicoChat;
import emprestimojogos.service.ServicoNotificacoes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Em produção, as mensagens chegam pelo endpoint WebSocket; aqui fica a regra de negócio. */
public class ServicoChatImpl implements ServicoChat {
    private final Repositorios repos;
    private final ServicoNotificacoes notificacoes;
    private final Clock clock;

    public ServicoChatImpl(Repositorios repos, ServicoNotificacoes notificacoes, Clock clock) {
        this.repos = repos;
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    @Override
    public Conversa iniciarConversa(int idInteressado, int idDono, int idJogo) {
        Conversa conversa = repos.conversas().salvar(
                new Conversa(idInteressado, idDono, idJogo, LocalDateTime.now(clock)));
        notificarInteresse(idInteressado, idJogo);
        return conversa;
    }

    @Override
    public void notificarInteresse(int idInteressado, int idJogo) {
        Jogo jogo = repos.jogos().obter(idJogo);
        String apelido = repos.usuarios().buscarPorId(idInteressado).map(Usuario::getApelido).orElse("Um usuário");
        notificacoes.notificar(jogo.getIdProprietario(), apelido + " tem interesse no seu jogo \""
                + jogo.getNome() + "\". Abra o chat para negociar.");
    }

    @Override
    public List<Mensagem> abrirChat(int idUsuario, int idConversa) {
        Conversa conversa = obterConversa(idConversa);
        verificarParticipante(idUsuario, conversa);
        return conversa.getMensagens();
    }

    @Override
    public Mensagem enviarMensagem(int idRemetente, int idConversa, String texto) {
        Conversa conversa = obterConversa(idConversa);
        verificarParticipante(idRemetente, conversa);
        if (!conversa.isAberta()) {
            throw new RegraNegocioException("A conversa foi encerrada.");
        }
        if (Texto.vazio(texto)) {
            throw new ValidacaoException("Mensagem vazia.", List.of("texto"));
        }
        // RNF01: antes do registro do empréstimo o chat não expõe contato
        String conteudo = conversa.isNegociacaoConcluida() ? texto.trim() : Texto.mascararContato(texto.trim());
        Mensagem mensagem = new Mensagem(idRemetente, conteudo, LocalDateTime.now(clock));
        conversa.adicionarMensagem(mensagem);
        repos.conversas().salvar(conversa);
        notificacoes.notificar(conversa.outroParticipante(idRemetente), "Você recebeu uma nova mensagem privada.");
        return mensagem;
    }

    @Override
    public void vincularEmprestimo(int idConversa, int idEmprestimo) {
        Conversa conversa = obterConversa(idConversa);
        conversa.setIdEmprestimo(idEmprestimo);
        repos.conversas().salvar(conversa);
    }

    @Override
    public void concluirNegociacao(int idConversa) {
        Conversa conversa = obterConversa(idConversa);
        conversa.setNegociacaoConcluida(true);
        repos.conversas().salvar(conversa);
    }

    @Override
    public void encerrarConversa(int idConversa, String motivo) {
        Conversa conversa = obterConversa(idConversa);
        conversa.encerrar(motivo);
        repos.conversas().salvar(conversa);
    }

    @Override
    public List<Integer> verificarInatividade() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Integer> emprestimosEncerrados = new ArrayList<>();
        for (Conversa c : repos.conversas().filtrar(c -> c.isAberta() && !c.isNegociacaoConcluida())) {
            if (c.getDataAvisoInatividade() == null) {
                if (c.ultimaMovimentacao().plusDays(RegrasNegocio.DIAS_INATIVIDADE_CHAT).isBefore(agora)) {
                    c.setDataAvisoInatividade(agora);
                    repos.conversas().salvar(c);
                    notificacoes.notificar(c.usuarioEmFalta(), "Há uma mensagem sem resposta há mais de "
                            + RegrasNegocio.DIAS_INATIVIDADE_CHAT + " dias. Sem nova movimentação em "
                            + RegrasNegocio.DIAS_ENCERRAMENTO_CHAT + " dias, a negociação será encerrada.");
                }
            } else if (!c.getDataAvisoInatividade().plusDays(RegrasNegocio.DIAS_ENCERRAMENTO_CHAT).isAfter(agora)) {
                c.encerrar("Encerrada por inatividade.");
                repos.conversas().salvar(c);
                notificacoes.notificar(c.getIdDono(), "A negociação foi encerrada por inatividade.");
                notificacoes.notificar(c.getIdInteressado(), "A negociação foi encerrada por inatividade.");
                if (c.getIdEmprestimo() != 0) {
                    emprestimosEncerrados.add(c.getIdEmprestimo());
                }
            }
        }
        return emprestimosEncerrados;
    }

    @Override
    public Conversa obterConversa(int idConversa) {
        return repos.conversas().obter(idConversa);
    }

    @Override
    public List<Conversa> listarConversas(int idUsuario) {
        return repos.conversas().filtrar(c -> c.participa(idUsuario));
    }

    private static void verificarParticipante(int idUsuario, Conversa conversa) {
        if (!conversa.participa(idUsuario)) {
            throw new AcessoNegadoException("Você não participa desta conversa.");
        }
    }
}
