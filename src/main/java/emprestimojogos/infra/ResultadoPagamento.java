package emprestimojogos.infra;

public record ResultadoPagamento(boolean aprovado, String idTransacao, String motivo) {
    public static ResultadoPagamento aprovado(String idTransacao) {
        return new ResultadoPagamento(true, idTransacao, null);
    }

    public static ResultadoPagamento recusado(String motivo) {
        return new ResultadoPagamento(false, null, motivo);
    }
}
