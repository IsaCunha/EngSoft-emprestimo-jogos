package emprestimojogos.infra;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Relógio que pode ser avançado manualmente. Permite simular a passagem dos dias
 * (prazos, multas, inatividade do chat) em testes e na demonstração.
 */
public class RelogioAjustavel extends Clock {
    private Instant instante;
    private final ZoneId zona;

    public RelogioAjustavel(Instant instante, ZoneId zona) {
        this.instante = instante;
        this.zona = zona;
    }

    public static RelogioAjustavel em(LocalDate data) {
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        return new RelogioAjustavel(data.atTime(LocalTime.of(10, 0)).atZone(zona).toInstant(), zona);
    }

    public void avancarDias(long dias) {
        instante = instante.plus(Duration.ofDays(dias));
    }

    public void avancarHoras(long horas) {
        instante = instante.plus(Duration.ofHours(horas));
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return new RelogioAjustavel(instante, novaZona);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
