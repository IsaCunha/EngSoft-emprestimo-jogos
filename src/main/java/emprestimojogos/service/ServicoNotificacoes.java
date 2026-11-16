package emprestimojogos.service;

import emprestimojogos.classes.Notificacao;

import java.util.List;

/** Notificações internas da plataforma (ex.: nova mensagem, interesse no jogo). */
public interface ServicoNotificacoes {
    void notificar(int idUsuario, String texto);

    List<Notificacao> listar(int idUsuario);
}
