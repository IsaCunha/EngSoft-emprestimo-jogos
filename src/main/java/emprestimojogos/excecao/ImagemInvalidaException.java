package emprestimojogos.excecao;

import java.util.List;

/** Imagem rejeitada pelo Processador de Imagens (formato, tamanho ou conteúdo inválido). */
public class ImagemInvalidaException extends ValidacaoException {
    private static final long serialVersionUID = 1L;
    public ImagemInvalidaException(String motivo) {
        super(motivo, List.of("imagem"));
    }
}
