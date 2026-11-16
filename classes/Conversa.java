package classes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Conversa do chat interno entre interessado e proprietário de um jogo. */
public class Conversa {
    private int idConversa;
    private int idEmprestimo;
    private final int idInteressado;
    private final int idDono;
    private final int idJogo;
    private final LocalDateTime dataCriacao;
    private final List<Mensagem> mensagens = new ArrayList<>();
    private boolean aberta = true;
    private boolean negociacaoConcluida = false;
    private LocalDateTime dataAvisoInatividade;
    private String motivoEncerramento;

    public Conversa(int idInteressado, int idDono, int idJogo, LocalDateTime dataCriacao) {
        this.idInteressado = idInteressado;
        this.idDono = idDono;
        this.idJogo = idJogo;
        this.dataCriacao = dataCriacao;
    }

    // Verifica se o usuário faz parte da conversa (validação de segurança)
    public boolean participa(int idUsuario) {
        return idUsuario == idInteressado || idUsuario == idDono;
    }

    // Recebe um usuário e indica o outro usuário da conversa
    // - Se você passa o ID do dono, ele te devolve o ID do interessado.
    // - Se você passa o ID do interessado, ele te devolve o ID do dono.
    public int outroParticipante(int idUsuario) {
        return idUsuario == idDono ? idInteressado : idDono;
    }

    public void adicionarMensagem(Mensagem mensagem) {
        mensagens.add(mensagem);
        dataAvisoInatividade = null;
    }

    public LocalDateTime ultimaMovimentacao() {
        return mensagens.isEmpty() ? dataCriacao : mensagens.get(mensagens.size() - 1).dataHora();
    }

    /** Quem deve responder: o destinatário da última mensagem (sem mensagens, o dono). */
    public int usuarioEmFalta() {
        return mensagens.isEmpty() ? idDono : outroParticipante(mensagens.get(mensagens.size() - 1).idRemetente());
    }

    public void encerrar(String motivo) {
        this.aberta = false;
        this.motivoEncerramento = motivo;
    }

    public int getIdConversa() { return idConversa; }
    public void setIdConversa(int idConversa) { this.idConversa = idConversa; }
    public int getIdEmprestimo() { return idEmprestimo; }
    public void setIdEmprestimo(int idEmprestimo) { this.idEmprestimo = idEmprestimo; }
    public int getIdInteressado() { return idInteressado; }
    public int getIdDono() { return idDono; }
    public int getIdJogo() { return idJogo; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public List<Mensagem> getMensagens() { return List.copyOf(mensagens); }
    public boolean isAberta() { return aberta; }
    public boolean isNegociacaoConcluida() { return negociacaoConcluida; }
    public void setNegociacaoConcluida(boolean concluida) { this.negociacaoConcluida = concluida; }
    public LocalDateTime getDataAvisoInatividade() { return dataAvisoInatividade; }
    public void setDataAvisoInatividade(LocalDateTime data) { this.dataAvisoInatividade = data; }
    public String getMotivoEncerramento() { return motivoEncerramento; }
}
