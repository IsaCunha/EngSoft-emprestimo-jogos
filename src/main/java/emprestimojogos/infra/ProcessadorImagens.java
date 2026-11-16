package emprestimojogos.infra;

import emprestimojogos.classes.Imagem;

/** Processador de Imagens (RNF02): valida, redimensiona e compacta antes de gravar. */
public interface ProcessadorImagens {
    Imagem processar(String nomeArquivo, byte[] conteudo);

    void excluir(Imagem imagem);
}
