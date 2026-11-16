package emprestimojogos.excecao;

import java.util.List;

/** Dados inválidos ou incompletos. Indica quais campos devem ser corrigidos. */
public class ValidacaoException extends SistemaException {
    private static final long serialVersionUID = 1L;
    private final transient List<String> campos;

    public ValidacaoException(String mensagem, List<String> campos) {
        super(mensagem + " Campos: " + campos);
        this.campos = List.copyOf(campos);
    }

    public List<String> getCampos() {
        return campos;
    }
}
