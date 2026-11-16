package classes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Transação de empréstimo entre proprietário e locatário. Nasce na manifestação
 * de interesse (SOLICITADO) e percorre os estados do Diagrama de Estados.
 */
public class Emprestimo {
    private int idEmprestimo;
    private final int idJogo;
    private final int idProprietario;
    private final int idLocatario;
    private final LocalDateTime dataSolicitacao;
    private int idConversa;

    private LocalDate prazo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private boolean devolvido = false;
    private StatusEmprestimo status = StatusEmprestimo.SOLICITADO;

    private double valorDiaria;
    private String codigoRetirada;
    private String codigoDevolucao;
    private LocalDate dataUltimoEmailConfirmacao;

    public Emprestimo(int idJogo, int idProprietario, int idLocatario, LocalDateTime dataSolicitacao) {
        this.idJogo = idJogo;
        this.idProprietario = idProprietario;
        this.idLocatario = idLocatario;
        this.dataSolicitacao = dataSolicitacao;
    }

    /** Altera o estado respeitando as transições do diagrama de estados. */
    public void alterarStatus(StatusEmprestimo novo) {
        if (!status.podeIrPara(novo)) {
            throw new RegraNegocioException("Transição de estado inválida: "
                    + status.getDescricao() + " -> " + novo.getDescricao());
        }
        this.status = novo;
    }

    /** Marca a devolução física do jogo na data informada. */
    public void registrarDevolucao(LocalDate data) {
        this.devolvido = true;
        this.dataFim = data;
    }

    /** Dias corridos após o prazo (até a devolução, se já ocorreu). */
    public long diasAtraso(LocalDate hoje) {
        if (prazo == null) {
            return 0;
        }
        LocalDate referencia = devolvido && dataFim != null ? dataFim : hoje;
        return Math.max(0, ChronoUnit.DAYS.between(prazo, referencia));
    }

    /** Custo previsto do empréstimo: diária x dias acordados. */
    public double custoPrevisto() {
        if (prazo == null || dataInicio == null) {
            return 0;
        }
        long dias = Math.max(1, ChronoUnit.DAYS.between(dataInicio, prazo));
        return RegrasNegocio.arredondar(valorDiaria * dias);
    }

    // Validação de segurança
    public boolean envolve(int idUsuario) {
        return idUsuario == idProprietario || idUsuario == idLocatario;
    }

    public int getIdEmprestimo() { return idEmprestimo; }
    public void setIdEmprestimo(int idEmprestimo) { this.idEmprestimo = idEmprestimo; }
    public int getIdJogo() { return idJogo; }
    public int getIdProprietario() { return idProprietario; }
    public int getIdLocatario() { return idLocatario; }
    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public int getIdConversa() { return idConversa; }
    public void setIdConversa(int idConversa) { this.idConversa = idConversa; }
    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public boolean isDevolvido() { return devolvido; }
    public StatusEmprestimo getStatus() { return status; }
    public double getValorDiaria() { return valorDiaria; }
    public void setValorDiaria(double valorDiaria) { this.valorDiaria = valorDiaria; }
    public String getCodigoRetirada() { return codigoRetirada; }
    public void setCodigoRetirada(String codigo) { this.codigoRetirada = codigo; }
    public String getCodigoDevolucao() { return codigoDevolucao; }
    public void setCodigoDevolucao(String codigo) { this.codigoDevolucao = codigo; }
    public LocalDate getDataUltimoEmailConfirmacao() { return dataUltimoEmailConfirmacao; }
    public void setDataUltimoEmailConfirmacao(LocalDate data) { this.dataUltimoEmailConfirmacao = data; }
}
