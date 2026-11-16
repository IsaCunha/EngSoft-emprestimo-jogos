package emprestimojogos.infra;

import java.util.OptionalDouble;

/** UC03: o sistema vincula ao jogo o seu valor médio de mercado via API externa. */
public interface ConsultaValorMercado {
    OptionalDouble consultarValor(String nomeJogo);
}
