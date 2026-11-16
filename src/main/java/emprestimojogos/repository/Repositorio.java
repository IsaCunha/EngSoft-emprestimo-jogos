package emprestimojogos.repository;

import emprestimojogos.excecao.NaoEncontradoException;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Acesso a dados isolado dos serviços (RNF06, RNF08). A implementação em memória
 * pode ser trocada por uma implementação JDBC/SQLite sem alterar os serviços.
 */
public interface Repositorio<T> {
    T salvar(T entidade);

    Optional<T> buscarPorId(int id);

    List<T> listarTodos();

    List<T> filtrar(Predicate<T> criterio);

    void remover(int id);

    String nomeEntidade();

    default T obter(int id) {
        return buscarPorId(id).orElseThrow(
                () -> new NaoEncontradoException(nomeEntidade() + " " + id + " não encontrado(a)."));
    }
}
