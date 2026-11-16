package emprestimojogos.excecao;

/** O usuário não tem permissão para a operação (ex.: não é o dono do jogo). */
public class AcessoNegadoException extends SistemaException {
    private static final long serialVersionUID = 1L;
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
