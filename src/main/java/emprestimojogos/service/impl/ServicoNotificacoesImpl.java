package emprestimojogos.service.impl;

import emprestimojogos.classes.Notificacao;
import emprestimojogos.repository.Repositorio;
import emprestimojogos.service.ServicoNotificacoes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class ServicoNotificacoesImpl implements ServicoNotificacoes {
    private final Repositorio<Notificacao> notificacoes;
    private final Clock clock;

    public ServicoNotificacoesImpl(Repositorio<Notificacao> notificacoes, Clock clock) {
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    @Override
    public void notificar(int idUsuario, String texto) {
        notificacoes.salvar(new Notificacao(idUsuario, texto, LocalDateTime.now(clock)));
    }

    @Override
    public List<Notificacao> listar(int idUsuario) {
        return notificacoes.filtrar(n -> n.getIdUsuario() == idUsuario);
    }
}
