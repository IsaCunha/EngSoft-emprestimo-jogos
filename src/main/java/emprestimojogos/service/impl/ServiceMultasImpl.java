package emprestimojogos.service.impl;

import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.infra.GatewayPagamento;
import emprestimojogos.infra.ResultadoPagamento;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Multa;
import emprestimojogos.classes.RegrasNegocio;
import emprestimojogos.classes.StatusConta;
import emprestimojogos.classes.StatusEmprestimo;
import emprestimojogos.classes.TipoMulta;
import emprestimojogos.classes.Usuario;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.ResultadoPagamentoMulta;
import emprestimojogos.service.ResumoPendencia;
import emprestimojogos.service.ServiceMultas;
import emprestimojogos.service.ServiceUsuario;
import emprestimojogos.service.ServicoNotificacoes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ServiceMultasImpl implements ServiceMultas {
    private final Repositorios repos;
    private final ServiceUsuario serviceUsuario;
    private final GatewayPagamento gateway;
    private final ServicoNotificacoes notificacoes;
    private final Clock clock;

    public ServiceMultasImpl(Repositorios repos, ServiceUsuario serviceUsuario, GatewayPagamento gateway,
                             ServicoNotificacoes notificacoes, Clock clock) {
        this.repos = repos;
        this.serviceUsuario = serviceUsuario;
        this.gateway = gateway;
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    @Override
    public void aplicarMulta(int idUsuario, int idMulta) {
        serviceUsuario.obterUsuario(idUsuario);
        Multa multa = repos.multas().obter(idMulta);
        multa.setIdUsuario(idUsuario);
        repos.multas().salvar(multa);
        serviceUsuario.bloquear(idUsuario); // FA02: se já estiver bloqueado, apenas vincula o débito
        notificacoes.notificar(idUsuario, String.format(RegrasNegocio.LOCALE_BR,
                "Multa de R$ %.2f registrada. Seu acesso a novos empréstimos está bloqueado.", multa.getValor()));
    }

    @Override
    public void editarMulta(int idMulta, Multa multa) {
        Multa existente = repos.multas().obter(idMulta);
        if (multa.getValor() < existente.getValorPago()) {
            throw new ValidacaoException("O valor da multa não pode ser menor que o já pago.", List.of("valor"));
        }
        existente.setValor(multa.getValor());
        existente.setDataVencimento(multa.getDataVencimento());
        existente.setTipo(multa.getTipo());
        if (multa.isPaga()) {
            existente.setPaga();
        }
        if (multa.isCancelada()) {
            existente.cancelar();
        }
        repos.multas().salvar(existente);
    }

    @Override
    public Multa gerarMulta(int idU, int idE, double valor) {
        return gerarMulta(idU, idE, valor, TipoMulta.DIARIA);
    }

    @Override
    public Multa gerarMulta(int idU, int idE, double valor, TipoMulta tipo) {
        if (valor < 0) {
            throw new ValidacaoException("Valor de multa inválido.", List.of("valor"));
        }
        repos.emprestimos().obter(idE);
        LocalDate vencimento = LocalDate.now(clock).plusDays(RegrasNegocio.DIAS_VENCIMENTO_MULTA);
        return repos.multas().salvar(new Multa(idE, idU, RegrasNegocio.arredondar(valor), tipo, vencimento));
    }

    @Override
    public Optional<Multa> obterMultaPendente(int idEmprestimo, TipoMulta tipo) {
        return repos.multas().filtrar(m -> m.getIdEmprestimo() == idEmprestimo && m.getTipo() == tipo
                && m.isPendente()).stream().findFirst();
    }

    @Override
    public List<Multa> listarPendentes(int idUsuario) {
        return repos.multas().filtrar(m -> m.getIdUsuario() == idUsuario && m.isPendente());
    }

    @Override
    public boolean possuiPendencias(int idUsuario) {
        return !listarPendentes(idUsuario).isEmpty()
                || !repos.emprestimos().filtrar(e -> e.getIdLocatario() == idUsuario
                && e.getStatus().isInadimplente()).isEmpty();
    }

    @Override
    public List<ResumoPendencia> resumoPendencias(int idUsuario) {
        LocalDate hoje = LocalDate.now(clock);
        return listarPendentes(idUsuario).stream().map(m -> {
            Emprestimo e = repos.emprestimos().obter(m.getIdEmprestimo());
            String nomeJogo = repos.jogos().buscarPorId(e.getIdJogo()).map(Jogo::getNome).orElse("(jogo removido)");
            return new ResumoPendencia(m.getIdMulta(), e.getIdEmprestimo(), nomeJogo, m.getTipo(), e.diasAtraso(hoje),
                    m.getValor(), m.getValorPago(), m.getSaldoDevedor(), e.isDevolvido());
        }).toList();
    }

    @Override
    public ResultadoPagamentoMulta pagarMulta(int idUsuario, int idMulta) {
        Multa multa = repos.multas().obter(idMulta);
        if (multa.getIdUsuario() != idUsuario) {
            throw new AcessoNegadoException("Esta multa não pertence ao usuário.");
        }
        if (!multa.isPendente()) {
            throw new RegraNegocioException("Esta multa já foi quitada.");
        }
        Emprestimo emprestimo = repos.emprestimos().obter(multa.getIdEmprestimo());

        double saldo = multa.getSaldoDevedor();
        if (saldo > 0) {
            ResultadoPagamento resultado = gateway.cobrar(idUsuario, saldo);
            if (!resultado.aprovado()) { // FA02: pendências e restrições inalteradas
                return new ResultadoPagamentoMulta(false, 0, false, "Pagamento não confirmado ("
                        + resultado.motivo() + "). As pendências e restrições da conta foram mantidas.");
            }
            multa.registrarPagamento(saldo);
            repassarAoProprietario(multa, emprestimo, saldo);
        }

        String mensagem;
        if (multa.getTipo() == TipoMulta.DIARIA) {
            if (emprestimo.isDevolvido()) {
                multa.setPaga();
                if (emprestimo.getStatus() == StatusEmprestimo.COM_PENDENCIA) {
                    emprestimo.alterarStatus(StatusEmprestimo.CONFIRMACAO_ENVIADA);
                    repos.emprestimos().salvar(emprestimo);
                }
                mensagem = "Multa quitada.";
            } else { // FA03: paga o acumulado, mas o jogo ainda não voltou
                mensagem = String.format(RegrasNegocio.LOCALE_BR, "Pagamento de R$ %.2f registrado. O desbloqueio definitivo só ocorrerá "
                        + "após o proprietário confirmar a devolução do jogo.", saldo);
            }
        } else { // UC12 FA01: ressarcimento de jogo extraviado
            multa.setPaga();
            if (emprestimo.getStatus() == StatusEmprestimo.EXTRAVIADO) {
                emprestimo.alterarStatus(StatusEmprestimo.RESSARCIDO);
                repos.emprestimos().salvar(emprestimo);
            }
            notificacoes.notificar(emprestimo.getIdProprietario(),
                    "O valor de reposição do seu jogo foi pago e o crédito está disponível na sua conta.");
            mensagem = "Valor de reposição quitado. O proprietário foi ressarcido.";
        }
        repos.multas().salvar(multa);

        reavaliarSituacao(idUsuario);
        Usuario usuario = serviceUsuario.obterUsuario(idUsuario);
        boolean desbloqueada = usuario.getStatusConta() == StatusConta.ATIVA;
        if (desbloqueada) {
            mensagem += usuario.isReputacaoBaixa()
                    ? " Conta restaurada com aviso de reputação baixa e limite de 1 empréstimo simultâneo."
                    : " Conta desbloqueada.";
        }
        return new ResultadoPagamentoMulta(true, saldo, desbloqueada, mensagem);
    }

    @Override
    public void reavaliarSituacao(int idUsuario) {
        Usuario usuario = serviceUsuario.obterUsuario(idUsuario);
        boolean extraviadoPendente = !repos.emprestimos().filtrar(e -> e.getIdLocatario() == idUsuario
                && e.getStatus() == StatusEmprestimo.EXTRAVIADO).isEmpty();
        if (extraviadoPendente) {
            serviceUsuario.suspender(idUsuario);
        } else if (possuiPendencias(idUsuario)) {
            if (usuario.getStatusConta() == StatusConta.SUSPENSA) {
                serviceUsuario.desbloquear(idUsuario);
            }
            serviceUsuario.bloquear(idUsuario);
        } else {
            serviceUsuario.desbloquear(idUsuario);
        }
    }

    @Override
    public void marcarMultasVencidas() {
        LocalDate hoje = LocalDate.now(clock);
        for (Multa m : repos.multas().filtrar(m -> m.isPendente() && !m.isVencida()
                && m.getDataVencimento() != null && m.getDataVencimento().isBefore(hoje))) {
            m.setVencida();
            repos.multas().salvar(m);
        }
    }

    /**
     * A multa diária é a diária (repassada ao dono) + a porcentagem da plataforma.
     * A reposição é o valor do jogo (repassado ao dono) + a multa administrativa.
     */
    private void repassarAoProprietario(Multa multa, Emprestimo emprestimo, double valorPago) {
        double percentualPlataforma = multa.getTipo() == TipoMulta.DIARIA
                ? RegrasNegocio.PERCENTUAL_PLATAFORMA : RegrasNegocio.PERCENTUAL_MULTA_ADMINISTRATIVA;
        double repasse = RegrasNegocio.arredondar(valorPago / (1 + percentualPlataforma));
        serviceUsuario.creditar(emprestimo.getIdProprietario(), repasse);
    }
}
