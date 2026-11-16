package classes;

/** Situação da conta de um usuário em relação a pendências (RF12-RF14). */
public enum StatusConta {
    // Pode solicitar novos empréstimos.
    ATIVA,
    // Multa pendente: acesso a novos jogos bloqueado (UC11).
    BLOQUEADA,
    // "Suspensão por Inadimplência Crítica": jogo extraviado sem ressarcimento (UC11 FA01). 
    SUSPENSA
}
