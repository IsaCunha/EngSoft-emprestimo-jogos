package emprestimojogos.service;

import emprestimojogos.classes.Multa;
import emprestimojogos.classes.TipoMulta;

import java.util.List;
import java.util.Optional;

/** Serviço de Multas (RF12-RF14): multas, bloqueio e desbloqueio após regularização. */
public interface ServiceMultas {
    // ---- operações do diagrama de classes ----
    /** Vincula a multa ao usuário e bloqueia o acesso (UC11; FA02 mantém o bloqueio existente). */
    void aplicarMulta(int idUsuario, int idMulta);

    void editarMulta(int idMulta, Multa multa);

    Multa gerarMulta(int idU, int idE, double valor);

    // ---- complementares ----
    Multa gerarMulta(int idU, int idE, double valor, TipoMulta tipo);

    Optional<Multa> obterMultaPendente(int idEmprestimo, TipoMulta tipo);

    List<Multa> listarPendentes(int idUsuario);

    boolean possuiPendencias(int idUsuario);

    /** UC12 – resumo da área de pendências. */
    List<ResumoPendencia> resumoPendencias(int idUsuario);

    /** UC12 – pagamento de multa (fluxo normal, FA01, FA02 e FA03). */
    ResultadoPagamentoMulta pagarMulta(int idUsuario, int idMulta);

    /** Recalcula o status da conta: SUSPENSA (extravio), BLOQUEADA (pendências) ou ATIVA. */
    void reavaliarSituacao(int idUsuario);

    void marcarMultasVencidas();
}
