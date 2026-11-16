package emprestimojogos.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Repositório em memória com geração automática de identificadores. */
public class RepositorioMemoria<T> implements Repositorio<T> {
    private final Map<Integer, T> dados = new LinkedHashMap<>();
    private final AtomicInteger sequencia = new AtomicInteger();
    private final String nomeEntidade;
    private final ToIntFunction<T> obterId;
    private final ObjIntConsumer<T> definirId;

    public RepositorioMemoria(String nomeEntidade, ToIntFunction<T> obterId, ObjIntConsumer<T> definirId) {
        this.nomeEntidade = nomeEntidade;
        this.obterId = obterId;
        this.definirId = definirId;
    }

    @Override
    public synchronized T salvar(T entidade) {
        int id = obterId.applyAsInt(entidade);
        if (id == 0) {
            id = sequencia.incrementAndGet();
            definirId.accept(entidade, id);
        }
        dados.put(id, entidade);
        return entidade;
    }

    @Override
    public synchronized Optional<T> buscarPorId(int id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public synchronized List<T> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public synchronized List<T> filtrar(Predicate<T> criterio) {
        return dados.values().stream().filter(criterio).toList();
    }

    @Override
    public synchronized void remover(int id) {
        dados.remove(id);
    }

    @Override
    public String nomeEntidade() {
        return nomeEntidade;
    }
}
