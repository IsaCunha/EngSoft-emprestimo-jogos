package emprestimojogos.service;

import emprestimojogos.apoio.Cenario;
import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.ImagemInvalidaException;
import emprestimojogos.excecao.NaoEncontradoException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.classes.Anuncio;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UC03 Cadastrar jogo / UC05 Visualizar / UC06 Pesquisar")
class ServiceJogosTest {
    private Cenario cenario;
    private ServiceJogos service;
    private Usuario ana;
    private Usuario bruno;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        service = cenario.sistema.jogos();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
    }

    @Test
    @DisplayName("UC03: jogo salvo, disponível e com anúncio ativo no catálogo")
    void cadastraJogo() {
        Jogo jogo = cenario.novoJogo(ana, "Catan", 250.0);
        assertTrue(jogo.verificarDisponibilidade());
        Anuncio anuncio = cenario.sistema.anuncios().obterAnuncioDoJogo(jogo.getIdJogo()).orElseThrow();
        assertTrue(anuncio.getAtivo());
        assertEquals(1, service.listarCatalogo(bruno.getIdUsuario()).size());
    }

    @Test
    @DisplayName("UC03: o valor de mercado vem da API quando ela conhece o jogo")
    void valorDeMercadoViaApi() {
        cenario.precos.registrar("Azul", 320.0);
        Jogo jogo = service.cadastrarJogo(ana.getIdUsuario(),
                DadosJogo.semImagem("Azul", "Azulejos portugueses", 4, 45, "Nova", null));
        assertEquals(320.0, jogo.getValorEstimado(), 0.001);
    }

    @Test
    @DisplayName("UC03: sem valor da API nem informado, o campo é obrigatório")
    void valorObrigatorio() {
        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.cadastrarJogo(ana.getIdUsuario(),
                DadosJogo.semImagem("Jogo raro", "Descrição", 2, 30, null, null)));
        assertTrue(ex.getCampos().contains("valorEstimado"));
    }

    @Test
    @DisplayName("UC03: campos obrigatórios ausentes")
    void camposObrigatorios() {
        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.cadastrarJogo(ana.getIdUsuario(),
                DadosJogo.semImagem(" ", "", 0, 30, null, 100.0)));
        assertTrue(ex.getCampos().containsAll(List.of("nome", "descricao", "numeroJogadores")));
        assertTrue(service.listarMeusJogos(ana.getIdUsuario()).isEmpty());
    }

    @Test
    @DisplayName("UC03: imagem é redimensionada e vinculada ao anúncio")
    void cadastraComImagem() {
        Jogo jogo = service.cadastrarJogo(ana.getIdUsuario(), new DadosJogo("Dixit", "Cartas ilustradas", 6, 30,
                "Boa", 180.0, "dixit.png", Cenario.imagemPng(2048, 2048)));
        ItemCatalogo item = service.listarCatalogo(null).get(0);
        assertEquals(jogo.getIdJogo(), item.idJogo());
        assertNotNull(item.caminhoImagem());
        Anuncio anuncio = cenario.sistema.anuncios().obterAnuncioDoJogo(jogo.getIdJogo()).orElseThrow();
        assertEquals(1024, cenario.sistema.anuncios().obterImagensAnuncio(anuncio.getIdAnuncio()).get(0).getLargura());
    }

    @Test
    @DisplayName("UC03: imagem inválida é rejeitada e nada é salvo")
    void imagemInvalida() {
        DadosJogo dados = new DadosJogo("Dixit", "Cartas", 6, 30, "Boa", 180.0, "dixit.png", "texto".getBytes());
        assertThrows(ImagemInvalidaException.class, () -> service.cadastrarJogo(ana.getIdUsuario(), dados));
        assertTrue(service.listarMeusJogos(ana.getIdUsuario()).isEmpty());
        assertTrue(cenario.sistema.repositorios().anuncios().listarTodos().isEmpty());
    }

    @Test
    @DisplayName("UC03: conta suspensa não cadastra jogos")
    void contaSuspensa() {
        cenario.sistema.usuarios().suspender(ana.getIdUsuario());
        assertThrows(AcessoNegadoException.class, () -> cenario.novoJogo(ana, "Catan", 200.0));
    }

    @Test
    @DisplayName("UC05: catálogo do usuário logado não mostra os próprios jogos")
    void catalogoExcluiProprios() {
        cenario.novoJogo(ana, "Catan", 200.0);
        cenario.novoJogo(bruno, "War", 150.0);
        List<ItemCatalogo> catalogoAna = service.listarCatalogo(ana.getIdUsuario());
        assertEquals(1, catalogoAna.size());
        assertEquals("War", catalogoAna.get(0).nomeJogo());
        assertEquals(2, service.listarCatalogo(null).size()); // visitante vê tudo
    }

    @Test
    @DisplayName("UC05: catálogo vazio retorna lista vazia")
    void catalogoVazio() {
        assertTrue(service.listarCatalogo(null).isEmpty());
    }

    @Test
    @DisplayName("UC05: catálogo informa a situação de cada jogo")
    void situacaoNoCatalogo() {
        Jogo catan = cenario.novoJogo(ana, "Catan", 200.0);
        cenario.emprestimoRetirado(ana, bruno, catan, 7);
        ItemCatalogo item = service.listarCatalogo(null).get(0);
        assertFalse(item.disponivel());
        assertEquals("Emprestado", item.situacao());
    }

    @Test
    @DisplayName("UC06: busca ignora maiúsculas e acentos e procura também na descrição")
    void pesquisa() {
        cenario.novoJogo(ana, "Pandemia", 200.0);
        service.cadastrarJogo(ana.getIdUsuario(),
                DadosJogo.semImagem("Ticket to Ride", "Construa ferrovias pela América", 5, 60, null, 300.0));
        assertEquals(1, service.pesquisar("PANDEMIA", bruno.getIdUsuario()).size());
        assertEquals(1, service.pesquisar("america", bruno.getIdUsuario()).size());
        assertEquals(2, service.pesquisar("", bruno.getIdUsuario()).size());
    }

    @Test
    @DisplayName("UC06: nenhum resultado")
    void pesquisaSemResultado() {
        cenario.novoJogo(ana, "Pandemia", 200.0);
        assertTrue(service.pesquisar("xadrez", null).isEmpty());
    }

    @Test
    @DisplayName("UC04: só o dono edita o jogo")
    void editarJogo() {
        Jogo jogo = cenario.novoJogo(ana, "Catan", 200.0);
        DadosJogo novos = DadosJogo.semImagem("Catan 5ª edição", "Nova edição", 4, 75, "Nova", 260.0);
        assertThrows(AcessoNegadoException.class, () -> service.editarJogo(bruno.getIdUsuario(), jogo.getIdJogo(), novos));
        Jogo editado = service.editarJogo(ana.getIdUsuario(), jogo.getIdJogo(), novos);
        assertEquals("Catan 5ª edição", editado.getNome());
        assertEquals(260.0, editado.getValorEstimado(), 0.001);
    }

    @Test
    @DisplayName("Operações do diagrama: registrarJogo, procurarJogo, registrarEmprestimo/Devolucao")
    void operacoesDoDiagrama() {
        Jogo jogo = new Jogo(ana.getIdUsuario(), "Uno", "Cartas", 10, 20, "Usado", 30.0);
        service.registrarJogo(jogo);
        assertEquals(jogo, service.procurarJogo("uno"));
        assertThrows(NaoEncontradoException.class, () -> service.procurarJogo("Banco Imobiliário"));
        assertThrows(ValidacaoException.class,
                () -> service.registrarJogo(new Jogo(ana.getIdUsuario(), "", "x", 0, 0, null, 0)));

        service.registrarEmprestimo(bruno.getIdUsuario(), jogo.getIdJogo());
        assertFalse(service.obterJogo(jogo.getIdJogo()).isDisponivel());
        service.registrarDevolucao(bruno.getIdUsuario(), jogo.getIdJogo());
        assertTrue(service.obterJogo(jogo.getIdJogo()).isDisponivel());
    }
}
