package emprestimojogos.service;

import emprestimojogos.classes.TipoMulta;

/** Resumo exibido na área de pendências (UC12). */
public record ResumoPendencia(int idMulta, int idEmprestimo, String nomeJogo, TipoMulta tipo, long diasAtraso,
                              double valorTotal, double valorPago, double saldoDevedor, boolean jogoDevolvido) {
}
