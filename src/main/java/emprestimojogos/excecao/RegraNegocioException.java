package emprestimojogos.excecao;

/** A operação viola uma regra de negócio (ex.: usuário bloqueado, jogo indisponível). */
public class RegraNegocioException extends SistemaException {
    private static final long serialVersionUID = 1L;
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
