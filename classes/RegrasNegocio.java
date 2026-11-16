package classes;

import java.util.Locale;

/** Parâmetros e cálculos de negócio definidos no relatório de modelagem. */
public final class RegrasNegocio {
    /** Formatação de valores em mensagens e e-mails (R$ 11,00). */
    public static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");
    /** UC09: a diária do empréstimo equivale a 5% do valor do jogo. */
    public static final double PERCENTUAL_DIARIA = 0.05;
    /** UC11: a multa diária é a diária acrescida da porcentagem da plataforma. */
    public static final double PERCENTUAL_PLATAFORMA = 0.10;
    /** UC11 FA01: multa administrativa somada ao valor de reposição, em % do valor do jogo. */
    public static final double PERCENTUAL_MULTA_ADMINISTRATIVA = 0.10;
    /** UC11: dias após o vencimento sem confirmação antes de aplicar a penalidade automaticamente. */
    public static final int DIAS_TOLERANCIA_CONFIRMACAO = 2;
    /** UC11 FA01: dias de atraso após os quais o jogo é considerado extraviado (relatório: 30-45). */
    public static final int DIAS_LIMITE_EXTRAVIO = 30;
    /** UC08: mensagem sem resposta por mais de 5 dias gera aviso. */
    public static final int DIAS_INATIVIDADE_CHAT = 5;
    /** UC08: após o aviso, 2 dias sem movimentação encerram a conversa. */
    public static final int DIAS_ENCERRAMENTO_CHAT = 2;
    /** UC12 FA01: conta restaurada após extravio fica limitada a 1 empréstimo simultâneo. */
    public static final int LIMITE_EMPRESTIMOS_REPUTACAO_BAIXA = 1;
    /** Prazo para pagamento de uma multa gerada. */
    public static final int DIAS_VENCIMENTO_MULTA = 30;

    private RegrasNegocio() {
    }

    public static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static double valorDiaria(double valorJogo) {
        return arredondar(valorJogo * PERCENTUAL_DIARIA);
    }

    public static double valorMultaDiaria(double valorDiaria) {
        return arredondar(valorDiaria * (1 + PERCENTUAL_PLATAFORMA));
    }

    public static double valorMultaAdministrativa(double valorJogo) {
        return arredondar(valorJogo * PERCENTUAL_MULTA_ADMINISTRATIVA);
    }

    public static double valorReposicao(double valorJogo) {
        return arredondar(valorJogo + valorMultaAdministrativa(valorJogo));
    }
}
