package emprestimojogos.service;

/**
 * Dados do formulário "Cadastrar jogo" (UC03).
 *
 * @param valorInformado usado só se a API de valor de mercado não encontrar o jogo (pode ser null)
 * @param imagem         conteúdo do arquivo de imagem (opcional)
 */
public record DadosJogo(String nome, String descricao, int numeroJogadores, int duracaoMediaMinutos,
                        String condicao, Double valorInformado, String nomeImagem, byte[] imagem) {

    public static DadosJogo semImagem(String nome, String descricao, int numeroJogadores,
                                      int duracaoMediaMinutos, String condicao, Double valorInformado) {
        return new DadosJogo(nome, descricao, numeroJogadores, duracaoMediaMinutos, condicao,
                valorInformado, null, null);
    }
}
