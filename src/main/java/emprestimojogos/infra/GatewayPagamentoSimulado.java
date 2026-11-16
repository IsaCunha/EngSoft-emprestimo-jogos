package emprestimojogos.infra;

import java.util.UUID;

/** Gateway de demonstração: aprova toda cobrança. */
public class GatewayPagamentoSimulado implements GatewayPagamento {
    @Override
    public ResultadoPagamento cobrar(int idUsuario, double valor) {
        return ResultadoPagamento.aprovado(UUID.randomUUID().toString());
    }
}
