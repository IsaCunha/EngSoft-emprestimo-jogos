package classes;

import java.time.LocalDateTime;

/** Notificação interna exibida ao usuário na plataforma. */
public class Notificacao {
    private int idNotificacao;
    private final int idUsuario;
    private final String texto;
    private final LocalDateTime dataHora;

    public Notificacao(int idUsuario, String texto, LocalDateTime dataHora) {
        this.idUsuario = idUsuario;
        this.texto = texto;
        this.dataHora = dataHora;
    }

    public int getIdNotificacao() { return idNotificacao; }
    public void setIdNotificacao(int id) { this.idNotificacao = id; }
    public int getIdUsuario() { return idUsuario; }
    public String getTexto() { return texto; }
    public LocalDateTime getDataHora() { return dataHora; }
}
