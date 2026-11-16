package emprestimojogos.service;

import emprestimojogos.classes.Conversa;
import emprestimojogos.classes.Mensagem;

import java.util.List;

/** Serviço de Chat (RF08; RNF01): mensagens com histórico, sem expor contato. */
public interface ServicoChat {
    /** Diagrama de sequência, mensagem 3. */
    Conversa iniciarConversa(int idInteressado, int idDono, int idJogo);

    /** Diagrama de sequência, mensagem 4. */
    void notificarInteresse(int idInteressado, int idJogo);

    /** Diagrama de sequência, mensagem 5: devolve o histórico de mensagens. */
    List<Mensagem> abrirChat(int idUsuario, int idConversa);

    Mensagem enviarMensagem(int idRemetente, int idConversa, String texto);

    void vincularEmprestimo(int idConversa, int idEmprestimo);

    void concluirNegociacao(int idConversa);

    void encerrarConversa(int idConversa, String motivo);

    /** UC08: avisa após 5 dias sem resposta e encerra 2 dias depois. Retorna os empréstimos encerrados. */
    List<Integer> verificarInatividade();

    Conversa obterConversa(int idConversa);

    List<Conversa> listarConversas(int idUsuario);
}
