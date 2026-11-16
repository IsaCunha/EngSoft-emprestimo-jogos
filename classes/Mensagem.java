package classes;

import java.time.LocalDateTime;

public record Mensagem(int idRemetente, String texto, LocalDateTime dataHora) {
}
