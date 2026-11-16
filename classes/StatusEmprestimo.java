package classes;

/**
 * Estados do ciclo de vida do empréstimo, conforme o Diagrama de Estados.
 * As transições permitidas estão em {@link #podeIrPara(StatusEmprestimo)}.
 */
public enum StatusEmprestimo {
    SOLICITADO("Solicitado"),
    EM_NEGOCIACAO("Em negociação"),
    NEGOCIACAO_CANCELADA("Negociação cancelada"),
    NEGOCIACAO_ACEITA("Negociação aceita"),
    AGUARDANDO_DEVOLUCAO("Aguardando devolução"),
    /** Estado final de sucesso: devolução confirmada. */
    CONFIRMACAO_ENVIADA("Confirmação enviada"),
    ATRASADO("Atrasado"),
    COM_PENDENCIA("Com pendência"),
    EXTRAVIADO("Extraviado / Não devolvido"),
    RESSARCIDO("Ressarcido");

    private final String descricao;

    StatusEmprestimo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean podeIrPara(StatusEmprestimo destino) {
        return switch (this) {
            case SOLICITADO -> destino == EM_NEGOCIACAO || destino == NEGOCIACAO_CANCELADA;
            case EM_NEGOCIACAO -> destino == NEGOCIACAO_ACEITA || destino == NEGOCIACAO_CANCELADA;
            // o empréstimo registrado ainda pode ser desfeito se o jogo nunca chegou a ser retirado
            case NEGOCIACAO_ACEITA -> destino == AGUARDANDO_DEVOLUCAO || destino == NEGOCIACAO_CANCELADA;
            case AGUARDANDO_DEVOLUCAO -> destino == CONFIRMACAO_ENVIADA || destino == ATRASADO;
            case ATRASADO -> destino == COM_PENDENCIA;
            case COM_PENDENCIA -> destino == CONFIRMACAO_ENVIADA || destino == EXTRAVIADO;
            case EXTRAVIADO -> destino == RESSARCIDO;
            case NEGOCIACAO_CANCELADA, CONFIRMACAO_ENVIADA, RESSARCIDO -> false;
        };
    }

    public boolean isFinal() {
        return this == NEGOCIACAO_CANCELADA || this == CONFIRMACAO_ENVIADA || this == RESSARCIDO;
    }

    public boolean isNegociacaoAberta() {
        return this == SOLICITADO || this == EM_NEGOCIACAO;
    }

    /** Empréstimo registrado e ainda não encerrado. Libera o contato entre as partes (RNF01). */
    public boolean isEmprestimoEmCurso() {
        return this == NEGOCIACAO_ACEITA || this == AGUARDANDO_DEVOLUCAO || this == ATRASADO
                || this == COM_PENDENCIA || this == EXTRAVIADO;
    }

    /** Estados que deixam o locatário inadimplente. */
    public boolean isInadimplente() {
        return this == ATRASADO || this == COM_PENDENCIA || this == EXTRAVIADO;
    }
}
