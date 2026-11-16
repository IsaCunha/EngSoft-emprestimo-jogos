package emprestimojogos.service.impl;

import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.ImagemInvalidaException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.infra.ProcessadorImagens;
import emprestimojogos.infra.Texto;
import emprestimojogos.classes.Anuncio;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Imagem;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.StatusEmprestimo;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.ServiceAnuncio;
import emprestimojogos.service.ServicoNotificacoes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ServiceAnuncioImpl implements ServiceAnuncio {
    private final Repositorios repos;
    private final ProcessadorImagens processadorImagens;
    private final ServicoNotificacoes notificacoes;

    public ServiceAnuncioImpl(Repositorios repos, ProcessadorImagens processadorImagens,
                              ServicoNotificacoes notificacoes) {
        this.repos = repos;
        this.processadorImagens = processadorImagens;
        this.notificacoes = notificacoes;
    }

    @Override
    public Anuncio gerarAnuncio(String titulo, String descricao) {
        validar(titulo, descricao);
        return repos.anuncios().salvar(new Anuncio(titulo.trim(), descricao.trim()));
    }

    @Override
    public Anuncio publicarAnuncio(int idJogo, int idDono, String titulo, String descricao) {
        Anuncio anuncio = gerarAnuncio(titulo, descricao);
        anuncio.vincular(idJogo, idDono);
        anuncio.setAtivo(true);
        return repos.anuncios().salvar(anuncio);
    }

    @Override
    public void addImagemAnuncio(int idImagem, int idAnuncio) {
        repos.imagens().obter(idImagem);
        Anuncio anuncio = repos.anuncios().obter(idAnuncio);
        if (!anuncio.getIdsImagens().contains(idImagem)) {
            anuncio.getIdsImagens().add(idImagem);
            repos.anuncios().salvar(anuncio);
        }
    }

    @Override
    public Imagem gerarImagem(String caminhoImg) {
        Path caminho = Path.of(caminhoImg);
        byte[] conteudo;
        try {
            conteudo = Files.readAllBytes(caminho);
        } catch (IOException e) {
            throw new ImagemInvalidaException("Não foi possível ler o arquivo de imagem.");
        }
        return gerarImagem(caminho.getFileName().toString(), conteudo);
    }

    @Override
    public Imagem gerarImagem(String nomeArquivo, byte[] conteudo) {
        return repos.imagens().salvar(processadorImagens.processar(nomeArquivo, conteudo));
    }

    @Override
    public void rmImagemAnuncio(int idAnuncio, int idImagem) {
        Anuncio anuncio = repos.anuncios().obter(idAnuncio);
        anuncio.getIdsImagens().remove(Integer.valueOf(idImagem));
        repos.anuncios().salvar(anuncio);
        repos.imagens().buscarPorId(idImagem).ifPresent(img -> {
            processadorImagens.excluir(img);
            repos.imagens().remover(idImagem);
        });
    }

    @Override
    public List<Imagem> obterImagensAnuncio(int idAnuncio) {
        Anuncio anuncio = repos.anuncios().obter(idAnuncio);
        List<Imagem> imagens = new ArrayList<>();
        for (int id : anuncio.getIdsImagens()) {
            repos.imagens().buscarPorId(id).ifPresent(imagens::add);
        }
        return imagens;
    }

    @Override
    public void removerAnuncio(int idAnuncio) {
        Anuncio anuncio = repos.anuncios().obter(idAnuncio);
        int idJogo = anuncio.getIdJogo();
        boolean emprestimoAtivo = !repos.emprestimos().filtrar(e -> e.getIdJogo() == idJogo
                && e.getStatus().isEmprestimoEmCurso()).isEmpty();
        if (emprestimoAtivo) {
            throw new RegraNegocioException("O jogo possui um empréstimo ativo e só pode ser removido "
                    + "após o fim do empréstimo e a devolução confirmada pelo dono.");
        }
        // negociações em aberto deixam de fazer sentido sem o anúncio
        for (Emprestimo e : repos.emprestimos().filtrar(e -> e.getIdJogo() == idJogo
                && e.getStatus().isNegociacaoAberta())) {
            e.alterarStatus(StatusEmprestimo.NEGOCIACAO_CANCELADA);
            repos.emprestimos().salvar(e);
            repos.conversas().buscarPorId(e.getIdConversa()).ifPresent(c -> {
                c.encerrar("Anúncio removido pelo dono.");
                repos.conversas().salvar(c);
            });
            notificacoes.notificar(e.getIdLocatario(), "O anúncio que você negociava foi removido pelo dono.");
        }
        for (int idImagem : List.copyOf(anuncio.getIdsImagens())) {
            rmImagemAnuncio(idAnuncio, idImagem);
        }
        repos.anuncios().remover(idAnuncio);
        if (idJogo != 0) {
            repos.jogos().remover(idJogo);
        }
    }

    @Override
    public void removerAnuncio(int idDono, int idAnuncio) {
        verificarDono(idDono, repos.anuncios().obter(idAnuncio));
        removerAnuncio(idAnuncio);
    }

    @Override
    public void desativarAnuncio(int idAnuncio) {
        Anuncio anuncio = repos.anuncios().obter(idAnuncio);
        anuncio.setAtivo(false);
        repos.anuncios().salvar(anuncio);
    }

    @Override
    public Anuncio editarAnuncio(int idDono, int idAnuncio, String titulo, String descricao) {
        Anuncio anuncio = repos.anuncios().obter(idAnuncio);
        verificarDono(idDono, anuncio);
        validar(titulo, descricao);
        anuncio.setTitulo(titulo.trim());
        anuncio.setDescricao(descricao.trim());
        return repos.anuncios().salvar(anuncio);
    }

    @Override
    public List<Anuncio> buscarAnuncio(String query, String campo) {
        Function<Anuncio, String> extrator = switch (Texto.normalizar(campo)) {
            case "titulo" -> Anuncio::getTitulo;
            case "descricao" -> Anuncio::getDescricao;
            case "jogo" -> a -> repos.jogos().buscarPorId(a.getIdJogo()).map(Jogo::getNome).orElse("");
            default -> throw new ValidacaoException("Campo de busca inválido. Use titulo, jogo ou descricao.",
                    List.of("campo"));
        };
        return repos.anuncios().filtrar(a -> a.getAtivo() && Texto.contem(extrator.apply(a), query));
    }

    @Override
    public Optional<Anuncio> obterAnuncioDoJogo(int idJogo) {
        return repos.anuncios().filtrar(a -> a.getIdJogo() == idJogo).stream().findFirst();
    }

    private static void validar(String titulo, String descricao) {
        List<String> erros = new ArrayList<>();
        if (Texto.vazio(titulo)) {
            erros.add("titulo");
        }
        if (Texto.vazio(descricao)) {
            erros.add("descricao");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException("Preencha os campos obrigatórios do anúncio.", erros);
        }
    }

    private static void verificarDono(int idUsuario, Anuncio anuncio) {
        if (anuncio.getIdDono() != idUsuario) {
            throw new AcessoNegadoException("Apenas o dono pode alterar este anúncio.");
        }
    }
}
