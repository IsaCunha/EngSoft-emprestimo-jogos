package emprestimojogos.excecao;

public class CredenciaisInvalidasException extends SistemaException {
    private static final long serialVersionUID = 1L;
    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
