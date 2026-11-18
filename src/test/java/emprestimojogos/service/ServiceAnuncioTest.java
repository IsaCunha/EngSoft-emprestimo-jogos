package emprestimojogos.service;

import emprestimojogos.apoio.Cenario;
import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.ImagemInvalidaException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.classes.Anuncio;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Imagem;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.StatusEmprestimo;
import emprestimojogos.classes.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UC04 Gerenciar anúncio")
class ServiceAnuncioTest {
    private Cenario cenario;
    private ServiceAnuncio service;
    private Usuario ana;
    private Usuario bruno;
    private Jogo catan;
    private Anuncio anuncio;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        service = cenario.sistema.anuncios();
        ana = cenario.novoUsuario("Ana");
        bruno = cenario.novoUsuario("Bruno");
        catan = cenario.novoJogo(ana, "Catan", 200.0);
        anuncio = service.obterAnuncioDoJogo(catan.getIdJogo()).orElseThrow();
    }

    @Test
    @DisplayName("Edição pelo dono atualiza título e descrição")
    void editar() {
        service.editarAnuncio(ana.getIdUsuario(), anuncio.getIdAnuncio(), "Catan completo", "Com expansão");
        assertEquals(1, service.buscarAnuncio("expansão", "descricao").size());
    }

    @Test
    @DisplayName("Outro usuário não edita nem remove o anúncio")
    void naoDono() {
        assertThrows(AcessoNegadoException.class,
                () -> service.editarAnuncio(bruno.getIdUsuario(), anuncio.getIdAnuncio(), "x", "y"));
        assertThrows(AcessoNegadoException.class,
                () -> service.removerAnuncio(bruno.getIdUsuario(), anuncio.getIdAnuncio()));
    }

    @Test
    @DisplayName("Remoção tira o anúncio e o jogo do catálogo")
    void remover() {
        service.removerAnuncio(ana.getIdUsuario(), anuncio.getIdAnuncio());
        assertTrue(cenario.sistema.jogos().listarCatalogo(null).isEmpty());
        assertTrue(cenario.sistema.jogos().listarMeusJogos(ana.getIdUsuario()).isEmpty());
    }

    @Test
    @DisplayName("Jogo com empréstimo ativo não pode ser removido")
    void removerComEmprestimoAtivo() {
        cenario.emprestimoRetirado(ana, bruno, catan, 7);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.removerAnuncio(ana.getIdUsuario(), anuncio.getIdAnuncio()));
        assertTrue(ex.getMessage().contains("empréstimo ativo"));
    }

    @Test
    @DisplayName("Após a devolução confirmada, a remoção é permitida")
    void removerAposDevolucao() {
        Emprestimo e = cenario.emprestimoRetirado(ana, bruno, catan, 7);
        cenario.devolver(ana, bruno, e);
        assertDoesNotThrow(() -> service.removerAnuncio(ana.getIdUsuario(), anuncio.getIdAnuncio()));
    }

    @Test
    @DisplayName("Remover o anúncio cancela negociações em aberto")
    void removerCancelaNegociacoes() {
        Emprestimo e = cenario.negociacao(bruno, catan);
        service.removerAnuncio(ana.getIdUsuario(), anuncio.getIdAnuncio());
        assertEquals(StatusEmprestimo.NEGOCIACAO_CANCELADA,
                cenario.sistema.emprestimos().obterEmprestimo(e.getIdEmprestimo()).getStatus());
    }

    @Test
    @DisplayName("Anúncio desativado sai do catálogo")
    void desativar() {
        service.desativarAnuncio(anuncio.getIdAnuncio());
        assertTrue(cenario.sistema.jogos().listarCatalogo(null).isEmpty());
    }

    @Test
    @DisplayName("gerarAnuncio exige título e descrição e nasce inativo")
    void gerarAnuncio() {
        Anuncio novo = service.gerarAnuncio("Título", "Descrição");
        assertFalse(novo.getAtivo());
        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.gerarAnuncio("", null));
        assertEquals(2, ex.getCampos().size());
    }

    @Test
    @DisplayName("Imagens: gerar a partir de um caminho, adicionar, listar e remover")
    void imagens() throws Exception {
        Path arquivo = Files.createTempFile("catan", ".png");
        Files.write(arquivo, Cenario.imagemPng(400, 300));

        Imagem imagem = service.gerarImagem(arquivo.toString());
        service.addImagemAnuncio(imagem.getIdImagem(), anuncio.getIdAnuncio());
        service.addImagemAnuncio(imagem.getIdImagem(), anuncio.getIdAnuncio()); // sem duplicar
        assertEquals(1, service.obterImagensAnuncio(anuncio.getIdAnuncio()).size());

        service.rmImagemAnuncio(anuncio.getIdAnuncio(), imagem.getIdImagem());
        assertTrue(service.obterImagensAnuncio(anuncio.getIdAnuncio()).isEmpty());
        assertFalse(Files.exists(Path.of(imagem.getCaminho())));
    }

    @Test
    @DisplayName("Imagem de caminho inexistente é rejeitada")
    void gerarImagemDeCaminhoInexistente() {
        assertThrows(ImagemInvalidaException.class, () -> service.gerarImagem("/nao/existe.png"));
    }

    @Test
    @DisplayName("buscarAnuncio por título, nome do jogo ou descrição")
    void buscarAnuncio() {
        cenario.novoJogo(bruno, "War", 150.0);
        assertEquals(1, service.buscarAnuncio("cat", "titulo").size());
        assertEquals(1, service.buscarAnuncio("war", "jogo").size());
        assertEquals(2, service.buscarAnuncio("descrição de", "descricao").size());
        assertThrows(ValidacaoException.class, () -> service.buscarAnuncio("x", "preco"));
    }
}
