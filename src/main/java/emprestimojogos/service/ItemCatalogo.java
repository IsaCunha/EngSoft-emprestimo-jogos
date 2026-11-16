package emprestimojogos.service;

/** Linha do catálogo (UC05/UC06): imagem, nome e situação do jogo. */
public record ItemCatalogo(int idJogo, int idAnuncio, String nomeJogo, String titulo, boolean disponivel,
                           String situacao, String caminhoImagem, String apelidoDono) {
}
