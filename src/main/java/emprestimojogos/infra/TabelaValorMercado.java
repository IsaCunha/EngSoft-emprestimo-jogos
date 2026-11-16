package emprestimojogos.infra;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/** Implementação local da consulta de valor de mercado, baseada em uma tabela de preços. */
public class TabelaValorMercado implements ConsultaValorMercado {
    private final Map<String, Double> precos = new HashMap<>();

    public TabelaValorMercado registrar(String nomeJogo, double valor) {
        precos.put(Texto.normalizar(nomeJogo), valor);
        return this;
    }

    @Override
    public OptionalDouble consultarValor(String nomeJogo) {
        Double valor = precos.get(Texto.normalizar(nomeJogo));
        return valor == null ? OptionalDouble.empty() : OptionalDouble.of(valor);
    }
}
