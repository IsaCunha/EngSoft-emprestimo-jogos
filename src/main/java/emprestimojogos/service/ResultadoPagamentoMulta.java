package emprestimojogos.service;

public record ResultadoPagamentoMulta(boolean aprovado, double valorPago, boolean contaDesbloqueada,
                                      String mensagem) {
}
