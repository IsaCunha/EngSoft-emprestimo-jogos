package emprestimojogos.excecao;

public class NaoEncontradoException extends SistemaException {
    private static final long serialVersionUID = 1L;
    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
