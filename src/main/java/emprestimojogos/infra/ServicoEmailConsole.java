package emprestimojogos.infra;

/** Implementação de demonstração que apenas imprime os e-mails no console. */
public class ServicoEmailConsole implements ServicoEmail {
    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        System.out.println("[E-MAIL] Para: " + destinatario + " | Assunto: " + assunto);
        System.out.println("         " + corpo.replace("\n", "\n         "));
    }
}
