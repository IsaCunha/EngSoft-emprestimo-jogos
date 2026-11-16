package emprestimojogos.classes;

import java.time.LocalDate;

/** Penalidade financeira vinculada a um empréstimo e a um usuário inadimplente. */
public class Multa {
    private int idMulta;
    private final int idEmprestimo;
    private int idUsuario;
    private double valor;
    private double valorPago;
    private boolean paga = false;
    private boolean vencida = false;
    private boolean cancelada = false;
    private LocalDate dataVencimento;
    private TipoMulta tipo;

    public Multa(int idEmprestimo, int idUsuario, double valor, TipoMulta tipo, LocalDate dataVencimento) {
        this.idEmprestimo = idEmprestimo;
        this.idUsuario = idUsuario;
        this.valor = valor;
        this.tipo = tipo;
        this.dataVencimento = dataVencimento;
    }

    public void setPaga() {
        this.paga = true;
    }

    public void setVencida() {
        this.vencida = true;
    }

    public void cancelar() {
        this.cancelada = true;
    }

    public void registrarPagamento(double valorPagamento) {
        this.valorPago = RegrasNegocio.arredondar(valorPago + valorPagamento);
    }

    public double getSaldoDevedor() {
        return Math.max(0, RegrasNegocio.arredondar(valor - valorPago));
    }

    /** Ainda conta como pendência do usuário. */
    public boolean isPendente() {
        return !paga && !cancelada;
    }

    public int getIdMulta() { return idMulta; }
    public void setIdMulta(int idMulta) { this.idMulta = idMulta; }
    public int getIdEmprestimo() { return idEmprestimo; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = RegrasNegocio.arredondar(valor); }
    public double getValorPago() { return valorPago; }
    public boolean isPaga() { return paga; }
    public boolean isVencida() { return vencida; }
    public boolean isCancelada() { return cancelada; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public TipoMulta getTipo() { return tipo; }
    public void setTipo(TipoMulta tipo) { this.tipo = tipo; }
}
