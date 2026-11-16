package emprestimojogos.infra;

/** Integração com o meio de pagamento usado para quitar multas (UC12). */
public interface GatewayPagamento {
    ResultadoPagamento cobrar(int idUsuario, double valor);
}
