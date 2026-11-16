package emprestimojogos.repository;

import emprestimojogos.classes.Anuncio;
import emprestimojogos.classes.Conversa;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Imagem;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Multa;
import emprestimojogos.classes.Notificacao;
import emprestimojogos.classes.Usuario;

/** Agrupa os repositórios da camada de persistência. */
public record Repositorios(
        Repositorio<Usuario> usuarios,
        Repositorio<Jogo> jogos,
        Repositorio<Anuncio> anuncios,
        Repositorio<Imagem> imagens,
        Repositorio<Emprestimo> emprestimos,
        Repositorio<Multa> multas,
        Repositorio<Conversa> conversas,
        Repositorio<Notificacao> notificacoes) {

    public static Repositorios emMemoria() {
        return new Repositorios(
                new RepositorioMemoria<>("Usuário", Usuario::getIdUsuario, Usuario::setIdUsuario),
                new RepositorioMemoria<>("Jogo", Jogo::getIdJogo, Jogo::setIdJogo),
                new RepositorioMemoria<>("Anúncio", Anuncio::getIdAnuncio, Anuncio::setIdAnuncio),
                new RepositorioMemoria<>("Imagem", Imagem::getIdImagem, Imagem::setIdImagem),
                new RepositorioMemoria<>("Empréstimo", Emprestimo::getIdEmprestimo, Emprestimo::setIdEmprestimo),
                new RepositorioMemoria<>("Multa", Multa::getIdMulta, Multa::setIdMulta),
                new RepositorioMemoria<>("Conversa", Conversa::getIdConversa, Conversa::setIdConversa),
                new RepositorioMemoria<>("Notificação", Notificacao::getIdNotificacao, Notificacao::setIdNotificacao));
    }
}
