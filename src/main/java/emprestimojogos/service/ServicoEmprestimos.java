package emprestimojogos.service;

import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.StatusEmprestimo;

import java.time.LocalDate;
import java.util.List;

/** Serviço de Empréstimos (RF07, RF09, RF11, RF12; RNF01). */
public interface ServicoEmprestimos {
    /** UC07 – Manifestar interesse (diagrama de sequência). Cria o empréstimo e a conversa. */
    Emprestimo manifestaInteresse(int idInteressado, int idJogo);

    /** UC08 (alternativo) – encerra a negociação sem empréstimo. */
    void cancelarNegociacao(int idUsuario, int idEmprestimo);

    /** UC09 – o dono registra o empréstimo com o prazo acordado. */
    Emprestimo registrarEmprestimo(int idDono, int idEmprestimo, int idInteressado, LocalDate prazo);

    /** Código que o dono mostra ao interessado na entrega do jogo. */
    String obterCodigoRetirada(int idDono, int idEmprestimo);

    /** UC09 – o interessado confirma o recebimento digitando o código de retirada. */
    void confirmarRecebimento(int idLocatario, int idEmprestimo, String codigo);

    /** Código que o locatário mostra ao dono na devolução do jogo. */
    String obterCodigoDevolucao(int idLocatario, int idEmprestimo);

    /** RF11 – envia ao dono o e-mail pedindo a confirmação da devolução. */
    void solicitarConfirmacaoDevolucao(int idEmprestimo);

    /** UC10 – o dono confirma a devolução digitando o código de devolução. */
    void confirmarDevolucao(int idDono, int idEmprestimo, String codigo);

    /** UC10 (alternativo) – o dono informa que o jogo não foi devolvido. */
    void informarNaoDevolucao(int idDono, int idEmprestimo);

    /** UC11 – Aplicar penalidade (executado pelo sistema). */
    void aplicarPenalidade(int idEmprestimo);

    /** Atualiza o valor acumulado da multa diária de um empréstimo em atraso. */
    void atualizarMultaDiaria(int idEmprestimo);

    /** UC11 FA01 – Extravio / não devolução definitiva. */
    void declararExtravio(int idEmprestimo);

    void cancelarPorInatividade(int idEmprestimo);

    /** RNF01 – contato da outra parte, liberado só após o registro do empréstimo. */
    Contato obterContato(int idSolicitante, int idEmprestimo);

    Emprestimo obterEmprestimo(int idEmprestimo);

    List<Emprestimo> listarEmprestimosDoUsuario(int idUsuario);

    List<Emprestimo> listarPorStatus(StatusEmprestimo status);
}
