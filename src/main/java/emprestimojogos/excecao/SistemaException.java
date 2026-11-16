package emprestimojogos.excecao;

/** Exceção base do sistema. Os controladores REST convertem cada subtipo em um código HTTP. */
public class SistemaException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public SistemaException(String mensagem) {
        super(mensagem);
    }

    public SistemaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
