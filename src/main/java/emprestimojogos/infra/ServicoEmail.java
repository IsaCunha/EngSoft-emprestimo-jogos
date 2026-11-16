package emprestimojogos.infra;

/** Serviço de E-mail (RF11). Em produção, implementado com Spring Mail via SMTP. */
public interface ServicoEmail {
    /** @throws RuntimeException se o envio falhar (o Agendador registra o erro e tenta de novo). */
    void enviar(String destinatario, String assunto, String corpo);
}
