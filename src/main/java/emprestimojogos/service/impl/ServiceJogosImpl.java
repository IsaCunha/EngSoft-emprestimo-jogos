package emprestimojogos.service.impl;

import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.NaoEncontradoException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.infra.ConsultaValorMercado;
import emprestimojogos.infra.Texto;
import emprestimojogos.classes.Anuncio;
import emprestimojogos.classes.Imagem;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.StatusConta;
import emprestimojogos.classes.Usuario;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.DadosJogo;
import emprestimojogos.service.ItemCatalogo;
import emprestimojogos.service.ServiceAnuncio;
import emprestimojogos.service.ServiceJogos;
import emprestimojogos.service.ServiceUsuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public class ServiceJogosImpl implements ServiceJogos {
    private final Repositorios repos;
    private final ServiceUsuario serviceUsuario;
    private final ServiceAnuncio serviceAnuncio;
    private final ConsultaValorMercado consultaValorMercado;

    public ServiceJogosImpl(Repositorios repos, ServiceUsuario serviceUsuario, ServiceAnuncio serviceAnuncio,
                            ConsultaValorMercado consultaValorMercado) {
        this.repos = repos;
        this.serviceUsuario = serviceUsuario;
        this.serviceAnuncio = serviceAnuncio;
        this.consultaValorMercado = consultaValorMercado;
    }

    @Override
    public void registrarJogo(Jogo jogo) {
        List<String> erros = new ArrayList<>();
        if (Texto.vazio(jogo.getNome())) {
            erros.add("nome");
        }
        if (jogo.getNumeroJogadores() < 1) {
            erros.add("numeroJogadores");
        }
        if (jogo.getValorEstimado() <= 0) {
            erros.add("valorEstimado");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException("Dados do jogo inválidos.", erros);
        }
        repos.jogos().salvar(jogo);
    }

    @Override
    public void registrarEmprestimo(int idUsuario, int idJogo) {
        Jogo jogo = obterJogo(idJogo);
        if (!jogo.verificarDisponibilidade()) {
            throw new RegraNegocioException("O jogo já está emprestado.");
        }
        jogo.setDisponivel(false);
        repos.jogos().salvar(jogo);
    }

    @Override
    public void registrarDevolucao(int idUsuario, int idJogo) {
        Jogo jogo = obterJogo(idJogo);
        jogo.setDisponivel(true);
        repos.jogos().salvar(jogo);
    }

    @Override
    public Jogo procurarJogo(String nome) {
        String alvo = Texto.normalizar(nome);
        List<Jogo> todos = repos.jogos().listarTodos();
        return todos.stream().filter(j -> Texto.normalizar(j.getNome()).equals(alvo)).findFirst()
                .or(() -> todos.stream().filter(j -> Texto.contem(j.getNome(), nome)).findFirst())
                .orElseThrow(() -> new NaoEncontradoException("Nenhum jogo encontrado com o nome \"" + nome + "\"."));
    }

    @Override
    public Jogo cadastrarJogo(int idDono, DadosJogo dados) {
        Usuario dono = serviceUsuario.obterUsuario(idDono);
        if (!dono.getAtivo() || dono.getStatusConta() == StatusConta.SUSPENSA) {
            throw new AcessoNegadoException("Conta sem permissão para cadastrar jogos.");
        }

        List<String> erros = validar(dados);
        OptionalDouble valorMercado = Texto.vazio(dados.nome())
                ? OptionalDouble.empty() : consultaValorMercado.consultarValor(dados.nome());
        double valor;
        if (valorMercado.isPresent()) {
            valor = valorMercado.getAsDouble();
        } else if (dados.valorInformado() != null && dados.valorInformado() > 0) {
            valor = dados.valorInformado();
        } else {
            erros.add("valorEstimado");
            valor = 0;
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException("Preencha os campos obrigatórios do jogo.", erros);
        }

        // a imagem é processada antes de gravar qualquer coisa: se for inválida, nada é salvo
        Imagem imagem = dados.imagem() == null ? null : serviceAnuncio.gerarImagem(dados.nomeImagem(), dados.imagem());

        String condicao = Texto.vazio(dados.condicao()) ? "Não informada" : dados.condicao().trim();
        Jogo jogo = new Jogo(idDono, dados.nome().trim(), dados.descricao().trim(), dados.numeroJogadores(),
                dados.duracaoMediaMinutos(), condicao, valor);
        registrarJogo(jogo);

        Anuncio anuncio = serviceAnuncio.publicarAnuncio(jogo.getIdJogo(), idDono, jogo.getNome(), jogo.getDescricao());
        if (imagem != null) {
            serviceAnuncio.addImagemAnuncio(imagem.getIdImagem(), anuncio.getIdAnuncio());
        }
        return jogo;
    }

    @Override
    public Jogo editarJogo(int idDono, int idJogo, DadosJogo dados) {
        Jogo jogo = obterJogo(idJogo);
        if (jogo.getIdProprietario() != idDono) {
            throw new AcessoNegadoException("Apenas o dono pode editar este jogo.");
        }
        List<String> erros = validar(dados);
        if (dados.valorInformado() != null && dados.valorInformado() <= 0) {
            erros.add("valorEstimado");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException("Dados do jogo inválidos.", erros);
        }
        jogo.setNome(dados.nome().trim());
        jogo.setDescricao(dados.descricao().trim());
        jogo.setNumeroJogadores(dados.numeroJogadores());
        jogo.setDuracaoMediaMinutos(dados.duracaoMediaMinutos());
        if (!Texto.vazio(dados.condicao())) {
            jogo.setCondicao(dados.condicao().trim());
        }
        if (dados.valorInformado() != null) {
            jogo.setValorEstimado(dados.valorInformado());
        }
        return repos.jogos().salvar(jogo);
    }

    @Override
    public List<ItemCatalogo> listarCatalogo(Integer idUsuarioLogado) {
        List<ItemCatalogo> itens = new ArrayList<>();
        for (Anuncio anuncio : repos.anuncios().filtrar(Anuncio::getAtivo)) {
            if (idUsuarioLogado != null && anuncio.getIdDono() == idUsuarioLogado) {
                continue;
            }
            Jogo jogo = repos.jogos().buscarPorId(anuncio.getIdJogo()).orElse(null);
            Usuario dono = repos.usuarios().buscarPorId(anuncio.getIdDono()).orElse(null);
            if (jogo == null || dono == null || !dono.getAtivo()) {
                continue;
            }
            String imagem = serviceAnuncio.obterImagensAnuncio(anuncio.getIdAnuncio()).stream()
                    .map(Imagem::getCaminho).findFirst().orElse(null);
            itens.add(new ItemCatalogo(jogo.getIdJogo(), anuncio.getIdAnuncio(), jogo.getNome(), anuncio.getTitulo(),
                    jogo.isDisponivel(), jogo.isDisponivel() ? "Disponível" : "Emprestado", imagem, dono.getApelido()));
        }
        return itens;
    }

    @Override
    public List<ItemCatalogo> pesquisar(String termo, Integer idUsuarioLogado) {
        List<ItemCatalogo> catalogo = listarCatalogo(idUsuarioLogado);
        if (Texto.vazio(termo)) {
            return catalogo;
        }
        return catalogo.stream().filter(item -> {
            String descricao = repos.anuncios().buscarPorId(item.idAnuncio())
                    .map(Anuncio::getDescricao).orElse("");
            return Texto.contem(item.nomeJogo(), termo) || Texto.contem(item.titulo(), termo)
                    || Texto.contem(descricao, termo);
        }).toList();
    }

    @Override
    public List<Jogo> listarMeusJogos(int idDono) {
        return repos.jogos().filtrar(j -> j.getIdProprietario() == idDono);
    }

    @Override
    public Jogo obterJogo(int idJogo) {
        return repos.jogos().obter(idJogo);
    }

    private static List<String> validar(DadosJogo dados) {
        Objects.requireNonNull(dados, "dados");
        List<String> erros = new ArrayList<>();
        if (Texto.vazio(dados.nome())) {
            erros.add("nome");
        }
        if (Texto.vazio(dados.descricao())) {
            erros.add("descricao");
        }
        if (dados.numeroJogadores() < 1) {
            erros.add("numeroJogadores");
        }
        if (dados.duracaoMediaMinutos() < 0) {
            erros.add("duracaoMediaMinutos");
        }
        return erros;
    }
}
