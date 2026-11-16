package emprestimojogos.service;

import emprestimojogos.classes.Anuncio;
import emprestimojogos.classes.Imagem;

import java.util.List;
import java.util.Optional;

/** Gestão de anúncios e imagens (RF03, RF06; RNF02). */
public interface ServiceAnuncio {
    // ---- operações do diagrama de classes ----
    Anuncio gerarAnuncio(String titulo, String descricao);

    void addImagemAnuncio(int idImagem, int idAnuncio);

    /** Lê o arquivo do caminho, processa (redimensiona/compacta) e registra a imagem. */
    Imagem gerarImagem(String caminhoImg);

    void rmImagemAnuncio(int idAnuncio, int idImagem);

    List<Imagem> obterImagensAnuncio(int idAnuncio);

    void removerAnuncio(int idAnuncio);

    void desativarAnuncio(int idAnuncio);

    /** @param campo "titulo", "jogo" ou "descricao" */
    List<Anuncio> buscarAnuncio(String query, String campo);

    // ---- complementares ----
    Imagem gerarImagem(String nomeArquivo, byte[] conteudo);

    Anuncio publicarAnuncio(int idJogo, int idDono, String titulo, String descricao);

    /** UC04 – edição feita pelo dono. */
    Anuncio editarAnuncio(int idDono, int idAnuncio, String titulo, String descricao);

    /** UC04 – remoção feita pelo dono. */
    void removerAnuncio(int idDono, int idAnuncio);

    Optional<Anuncio> obterAnuncioDoJogo(int idJogo);
}
