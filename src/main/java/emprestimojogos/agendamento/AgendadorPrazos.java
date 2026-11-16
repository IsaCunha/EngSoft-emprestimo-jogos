package emprestimojogos.agendamento;

import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.RegrasNegocio;
import emprestimojogos.classes.StatusEmprestimo;
import emprestimojogos.service.ServiceMultas;
import emprestimojogos.service.ServicoChat;
import emprestimojogos.service.ServicoEmprestimos;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Agendador de Prazos: ator temporal (RF10-RF13). Em produção, {@link #executar()} é
 * disparado periodicamente pelo Spring Scheduler (ex.: {@code @Scheduled(cron = "0 0 8 * * *")}).
 */
public class AgendadorPrazos {
    private final ServicoEmprestimos servicoEmprestimos;
    private final ServiceMultas serviceMultas;
    private final ServicoChat servicoChat;
    private final Clock clock;
    private final List<String> registroErros = new ArrayList<>();

    public AgendadorPrazos(ServicoEmprestimos servicoEmprestimos, ServiceMultas serviceMultas,
                           ServicoChat servicoChat, Clock clock) {
        this.servicoEmprestimos = servicoEmprestimos;
        this.serviceMultas = serviceMultas;
        this.servicoChat = servicoChat;
        this.clock = clock;
    }

    public void executar() {
        LocalDate hoje = LocalDate.now(clock);
        verificarVencimentos(hoje);
        atualizarAtrasos(hoje);
        serviceMultas.marcarMultasVencidas();
        for (int idEmprestimo : servicoChat.verificarInatividade()) {
            servicoEmprestimos.cancelarPorInatividade(idEmprestimo);
        }
    }

    /** RF10/RF11: no vencimento, pede confirmação ao dono por e-mail; após a tolerância, aplica a penalidade. */
    void verificarVencimentos(LocalDate hoje) {
        for (Emprestimo e : servicoEmprestimos.listarPorStatus(StatusEmprestimo.AGUARDANDO_DEVOLUCAO)) {
            if (e.getPrazo().isAfter(hoje)) {
                continue;
            }
            if (hoje.isAfter(e.getPrazo().plusDays(RegrasNegocio.DIAS_TOLERANCIA_CONFIRMACAO))) {
                servicoEmprestimos.aplicarPenalidade(e.getIdEmprestimo());
                continue;
            }
            // reenvio periódico: um e-mail por dia até a confirmação ou o fim da tolerância
            if (!hoje.equals(e.getDataUltimoEmailConfirmacao())) {
                try {
                    servicoEmprestimos.solicitarConfirmacaoDevolucao(e.getIdEmprestimo());
                } catch (RuntimeException ex) {
                    // registra o erro; como a data do último envio não foi gravada, a próxima execução tenta de novo
                    registroErros.add(hoje + " - falha ao enviar e-mail de confirmação do empréstimo "
                            + e.getIdEmprestimo() + ": " + ex.getMessage());
                }
            }
        }
    }

    /** RF12/RF13: acumula a multa diária e, passado o limite, declara o extravio. */
    void atualizarAtrasos(LocalDate hoje) {
        for (Emprestimo e : servicoEmprestimos.listarPorStatus(StatusEmprestimo.COM_PENDENCIA)) {
            if (e.isDevolvido()) {
                continue;
            }
            if (e.diasAtraso(hoje) > RegrasNegocio.DIAS_LIMITE_EXTRAVIO) {
                servicoEmprestimos.declararExtravio(e.getIdEmprestimo());
            } else {
                servicoEmprestimos.atualizarMultaDiaria(e.getIdEmprestimo());
            }
        }
    }

    public List<String> getRegistroErros() {
        return List.copyOf(registroErros);
    }
}
