package emprestimojogos.service.impl;

import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.infra.GeradorCodigo;
import emprestimojogos.infra.ServicoEmail;
import emprestimojogos.classes.Conversa;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Multa;
import emprestimojogos.classes.RegrasNegocio;
import emprestimojogos.classes.StatusEmprestimo;
import emprestimojogos.classes.TipoMulta;
import emprestimojogos.classes.Usuario;
import emprestimojogos.repository.Repositorio;
import emprestimojogos.service.Contato;
import emprestimojogos.service.ServiceJogos;
import emprestimojogos.service.ServiceMultas;
import emprestimojogos.service.ServiceUsuario;
import emprestimojogos.service.ServicoChat;
import emprestimojogos.service.ServicoEmprestimos;
import emprestimojogos.service.ServicoNotificacoes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServicoEmprestimosImpl implements ServicoEmprestimos {
    private final Repositorio<Emprestimo> emprestimos;
    private final ServiceUsuario serviceUsuario;
    private final ServiceJogos serviceJogos;
    private final ServiceMultas serviceMultas;
    private final ServicoChat servicoChat;
    private final ServicoEmail servicoEmail;
    private final ServicoNotificacoes notificacoes;
    private final Clock clock;
    private final List<String> errosEnvioEmail = new ArrayList<>();

    public ServicoEmprestimosImpl(Repositorio<Emprestimo> emprestimos, ServiceUsuario serviceUsuario,
                                  ServiceJogos serviceJogos, ServiceMultas serviceMultas, ServicoChat servicoChat,
                                  ServicoEmail servicoEmail, ServicoNotificacoes notificacoes, Clock clock) {
        this.emprestimos = emprestimos;
        this.serviceUsuario = serviceUsuario;
        this.serviceJogos = serviceJogos;
        this.serviceMultas = serviceMultas;
        this.servicoChat = servicoChat;
        this.servicoEmail = servicoEmail;
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ UC07 / UC08

    @Override
    public Emprestimo manifestaInteresse(int idInteressado, int idJogo) {
        Usuario interessado = serviceUsuario.obterUsuario(idInteressado);
        if (!interessado.getAtivo()) {
            throw new AcessoNegadoException("Conta desativada.");
        }
        if (interessado.isBloqueado()) {
            throw new RegraNegocioException("Seu acesso a novos empréstimos está bloqueado por pendência "
                    + "financeira. Regularize na área de pendências.");
        }
        Jogo jogo = serviceJogos.obterJogo(idJogo);
        if (jogo.getIdProprietario() == idInteressado) {
            throw new RegraNegocioException("Você não pode manifestar interesse no próprio jogo.");
        }
        if (!jogo.verificarDisponibilidade()) {
            throw new RegraNegocioException("O jogo está emprestado no momento.");
        }
        boolean jaNegociando = !emprestimos.filtrar(e -> e.getIdJogo() == idJogo
                && e.getIdLocatario() == idInteressado && e.getStatus().isNegociacaoAberta()).isEmpty();
        if (jaNegociando) {
            throw new RegraNegocioException("Você já manifestou interesse neste jogo.");
        }
        verificarLimiteEmprestimos(interessado);

        Emprestimo emprestimo = emprestimos.salvar(
                new Emprestimo(idJogo, jogo.getIdProprietario(), idInteressado, LocalDateTime.now(clock)));
        Conversa conversa = servicoChat.iniciarConversa(idInteressado, jogo.getIdProprietario(), idJogo);
        servicoChat.vincularEmprestimo(conversa.getIdConversa(), emprestimo.getIdEmprestimo());
        emprestimo.setIdConversa(conversa.getIdConversa());
        emprestimo.alterarStatus(StatusEmprestimo.EM_NEGOCIACAO);
        return emprestimos.salvar(emprestimo);
    }

    @Override
    public void cancelarNegociacao(int idUsuario, int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (!e.envolve(idUsuario)) {
            throw new AcessoNegadoException("Você não participa desta negociação.");
        }
        if (!e.getStatus().isNegociacaoAberta() && e.getStatus() != StatusEmprestimo.NEGOCIACAO_ACEITA) {
            throw new RegraNegocioException("Esta negociação não pode mais ser cancelada.");
        }
        if (e.getStatus() == StatusEmprestimo.NEGOCIACAO_ACEITA) {
            // registrado, mas o jogo nunca foi retirado: volta a ficar disponível
            serviceJogos.registrarDevolucao(e.getIdLocatario(), e.getIdJogo());
        }
        cancelar(e, "Negociação encerrada sem empréstimo.");
    }

    @Override
    public void cancelarPorInatividade(int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getStatus().isNegociacaoAberta()) {
            e.alterarStatus(StatusEmprestimo.NEGOCIACAO_CANCELADA);
            emprestimos.salvar(e);
        }
    }

    // ------------------------------------------------------------------ UC09

    @Override
    public Emprestimo registrarEmprestimo(int idDono, int idEmprestimo, int idInteressado, LocalDate prazo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getIdProprietario() != idDono) {
            throw new AcessoNegadoException("Apenas o dono do jogo pode registrar o empréstimo.");
        }
        if (e.getIdLocatario() != idInteressado) {
            throw new ValidacaoException("O interessado informado não corresponde à negociação.",
                    List.of("idInteressado"));
        }
        if (e.getStatus() != StatusEmprestimo.EM_NEGOCIACAO) {
            throw new RegraNegocioException("A negociação não está em andamento.");
        }
        LocalDate hoje = hoje();
        if (prazo == null || !prazo.isAfter(hoje)) {
            throw new ValidacaoException("Prazo de devolução inválido: informe uma data futura.", List.of("prazo"));
        }
        Usuario interessado = serviceUsuario.obterUsuario(idInteressado);
        if (interessado.isBloqueado()) {
            throw new RegraNegocioException("O interessado está bloqueado e não pode receber empréstimos.");
        }
        verificarLimiteEmprestimos(interessado);

        Jogo jogo = serviceJogos.obterJogo(e.getIdJogo());
        serviceJogos.registrarEmprestimo(idInteressado, jogo.getIdJogo()); // marca indisponível
        e.setPrazo(prazo);
        e.setValorDiaria(RegrasNegocio.valorDiaria(jogo.getValorEstimado()));
        e.setCodigoRetirada(GeradorCodigo.gerar());
        e.alterarStatus(StatusEmprestimo.NEGOCIACAO_ACEITA);
        emprestimos.salvar(e);
        servicoChat.concluirNegociacao(e.getIdConversa());

        for (Emprestimo outro : emprestimos.filtrar(o -> o.getIdJogo() == jogo.getIdJogo()
                && o.getIdEmprestimo() != idEmprestimo && o.getStatus().isNegociacaoAberta())) {
            cancelar(outro, "O jogo foi emprestado para outro usuário.");
        }
        notificacoes.notificar(idInteressado, "Empréstimo de \"" + jogo.getNome() + "\" registrado até " + prazo
                + ". Ao receber o jogo, digite o código de retirada informado pelo dono.");
        return e;
    }

    @Override
    public String obterCodigoRetirada(int idDono, int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getIdProprietario() != idDono) {
            throw new AcessoNegadoException("Apenas o dono vê o código de retirada.");
        }
        return e.getCodigoRetirada();
    }

    @Override
    public void confirmarRecebimento(int idLocatario, int idEmprestimo, String codigo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getIdLocatario() != idLocatario) {
            throw new AcessoNegadoException("Apenas o interessado confirma o recebimento.");
        }
        if (e.getStatus() != StatusEmprestimo.NEGOCIACAO_ACEITA) {
            throw new RegraNegocioException("Não há retirada pendente para este empréstimo.");
        }
        if (!GeradorCodigo.confere(e.getCodigoRetirada(), codigo)) {
            throw new RegraNegocioException("Código de retirada inválido.");
        }
        e.setDataInicio(hoje());
        e.setCodigoDevolucao(GeradorCodigo.gerar());
        e.alterarStatus(StatusEmprestimo.AGUARDANDO_DEVOLUCAO);
        emprestimos.salvar(e);
        notificacoes.notificar(e.getIdProprietario(), "O interessado confirmou o recebimento do jogo.");
    }

    @Override
    public String obterCodigoDevolucao(int idLocatario, int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getIdLocatario() != idLocatario) {
            throw new AcessoNegadoException("Apenas quem pegou o jogo vê o código de devolução.");
        }
        return e.getCodigoDevolucao();
    }

    // ------------------------------------------------------------------ UC10

    @Override
    public void solicitarConfirmacaoDevolucao(int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getStatus() != StatusEmprestimo.AGUARDANDO_DEVOLUCAO) {
            throw new RegraNegocioException("O empréstimo não está aguardando devolução.");
        }
        Usuario dono = serviceUsuario.obterUsuario(e.getIdProprietario());
        Jogo jogo = serviceJogos.obterJogo(e.getIdJogo());
        servicoEmail.enviar(dono.getEmail(), "Confirme a devolução de \"" + jogo.getNome() + "\"",
                "O prazo de devolução venceu em " + e.getPrazo() + ".\n"
                        + "Acesse /emprestimos/" + e.getIdEmprestimo() + "/devolucao e informe o código de devolução "
                        + "fornecido por quem pegou o jogo, ou informe que o jogo não foi devolvido.");
        e.setDataUltimoEmailConfirmacao(hoje()); // só marca se o envio não lançou exceção
        emprestimos.salvar(e);
    }

    @Override
    public void confirmarDevolucao(int idDono, int idEmprestimo, String codigo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getIdProprietario() != idDono) {
            throw new AcessoNegadoException("Apenas o dono confirma a devolução.");
        }
        StatusEmprestimo status = e.getStatus();
        if (status != StatusEmprestimo.AGUARDANDO_DEVOLUCAO && status != StatusEmprestimo.COM_PENDENCIA) {
            throw new RegraNegocioException("Não há devolução pendente para este empréstimo.");
        }
        if (e.isDevolvido()) {
            throw new RegraNegocioException("A devolução já foi confirmada.");
        }
        if (!GeradorCodigo.confere(e.getCodigoDevolucao(), codigo)) {
            throw new RegraNegocioException("Código de devolução inválido.");
        }

        e.registrarDevolucao(hoje());
        serviceJogos.registrarDevolucao(e.getIdLocatario(), e.getIdJogo());

        if (status == StatusEmprestimo.AGUARDANDO_DEVOLUCAO) {
            e.alterarStatus(StatusEmprestimo.CONFIRMACAO_ENVIADA);
            emprestimos.salvar(e);
            notificacoes.notificar(e.getIdLocatario(), "Devolução confirmada. Empréstimo encerrado.");
            return;
        }

        // devolução com atraso: fecha o valor final da multa diária
        emprestimos.salvar(e);
        Multa multa = atualizarValorMulta(e);
        if (multa == null || multa.getSaldoDevedor() <= 0) {
            if (multa != null) {
                multa.setPaga();
                serviceMultas.editarMulta(multa.getIdMulta(), multa);
            }
            e.alterarStatus(StatusEmprestimo.CONFIRMACAO_ENVIADA);
            emprestimos.salvar(e);
        } else {
            notificacoes.notificar(e.getIdLocatario(), String.format(RegrasNegocio.LOCALE_BR, "Devolução confirmada. Pague o saldo de "
                    + "R$ %.2f da multa para desbloquear sua conta.", multa.getSaldoDevedor()));
        }
        serviceMultas.reavaliarSituacao(e.getIdLocatario());
    }

    @Override
    public void informarNaoDevolucao(int idDono, int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getIdProprietario() != idDono) {
            throw new AcessoNegadoException("Apenas o dono pode informar a não devolução.");
        }
        if (e.getStatus() != StatusEmprestimo.AGUARDANDO_DEVOLUCAO) {
            throw new RegraNegocioException("O empréstimo não está aguardando devolução.");
        }
        if (hoje().isBefore(e.getPrazo())) {
            throw new RegraNegocioException("O prazo de devolução ainda não venceu.");
        }
        aplicarPenalidade(idEmprestimo);
    }

    // ------------------------------------------------------------------ UC11

    @Override
    public void aplicarPenalidade(int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        e.alterarStatus(StatusEmprestimo.ATRASADO);
        emprestimos.salvar(e);

        double multaDiaria = RegrasNegocio.valorMultaDiaria(e.getValorDiaria());
        long dias = Math.max(1, e.diasAtraso(hoje()));
        Multa multa = serviceMultas.gerarMulta(e.getIdLocatario(), idEmprestimo, multaDiaria * dias, TipoMulta.DIARIA);
        serviceMultas.aplicarMulta(e.getIdLocatario(), multa.getIdMulta()); // bloqueia (FA02: mantém)

        e.alterarStatus(StatusEmprestimo.COM_PENDENCIA);
        emprestimos.salvar(e);

        Jogo jogo = serviceJogos.obterJogo(e.getIdJogo());
        Usuario locatario = serviceUsuario.obterUsuario(e.getIdLocatario());
        LocalDate prazoLimite = e.getPrazo().plusDays(RegrasNegocio.DIAS_LIMITE_EXTRAVIO);
        enviarEmailSeguro(locatario.getEmail(), "Empréstimo em atraso: \"" + jogo.getNome() + "\"",
                String.format(RegrasNegocio.LOCALE_BR, "A devolução venceu em %s. Incide multa diária de R$ %.2f e seu acesso a novos "
                        + "empréstimos foi bloqueado. Se o jogo não for devolvido até %s, ele será considerado "
                        + "extraviado e será cobrado o valor de reposição.", e.getPrazo(), multaDiaria, prazoLimite));
        notificacoes.notificar(e.getIdProprietario(), "O empréstimo de \"" + jogo.getNome()
                + "\" foi marcado como atrasado e o usuário foi bloqueado.");
    }

    @Override
    public void atualizarMultaDiaria(int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getStatus() == StatusEmprestimo.COM_PENDENCIA && !e.isDevolvido()) {
            atualizarValorMulta(e);
        }
    }

    @Override
    public void declararExtravio(int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (e.getStatus() != StatusEmprestimo.COM_PENDENCIA || e.isDevolvido()) {
            throw new RegraNegocioException("Só um empréstimo em atraso e não devolvido pode ser declarado extraviado.");
        }
        // encerra as diárias: o que já foi pago fica registrado, o restante é convertido em reposição
        serviceMultas.obterMultaPendente(idEmprestimo, TipoMulta.DIARIA).ifPresent(diaria -> {
            if (diaria.getValorPago() > 0) {
                diaria.setValor(diaria.getValorPago());
                diaria.setPaga();
            } else {
                diaria.cancelar();
            }
            serviceMultas.editarMulta(diaria.getIdMulta(), diaria);
        });

        e.alterarStatus(StatusEmprestimo.EXTRAVIADO);
        emprestimos.salvar(e);

        Jogo jogo = serviceJogos.obterJogo(e.getIdJogo());
        double reposicao = RegrasNegocio.valorReposicao(jogo.getValorEstimado());
        Multa multa = serviceMultas.gerarMulta(e.getIdLocatario(), idEmprestimo, reposicao, TipoMulta.REPOSICAO);
        serviceMultas.aplicarMulta(e.getIdLocatario(), multa.getIdMulta());
        serviceUsuario.suspender(e.getIdLocatario());

        Usuario dono = serviceUsuario.obterUsuario(e.getIdProprietario());
        Usuario locatario = serviceUsuario.obterUsuario(e.getIdLocatario());
        enviarEmailSeguro(dono.getEmail(), "Jogo extraviado: \"" + jogo.getNome() + "\"",
                String.format(RegrasNegocio.LOCALE_BR, "O jogo foi dado como extraviado. Foi gerado um crédito de R$ %.2f a seu favor, "
                        + "disponibilizado assim que o valor de reposição for pago.", jogo.getValorEstimado()));
        enviarEmailSeguro(locatario.getEmail(), "Cobrança de reposição: \"" + jogo.getNome() + "\"",
                String.format(RegrasNegocio.LOCALE_BR, "O prazo limite foi excedido e o jogo foi considerado extraviado. Valor de reposição "
                        + "com multa administrativa: R$ %.2f. Sua conta está suspensa e a falta de pagamento "
                        + "poderá levar a procedimentos de restrição cadastral.", reposicao));
    }

    // ------------------------------------------------------------------ RNF01 e consultas

    @Override
    public Contato obterContato(int idSolicitante, int idEmprestimo) {
        Emprestimo e = obterEmprestimo(idEmprestimo);
        if (!e.envolve(idSolicitante)) {
            throw new AcessoNegadoException("Você não participa deste empréstimo.");
        }
        if (!e.getStatus().isEmprestimoEmCurso()) {
            throw new AcessoNegadoException("Os dados de contato só são liberados após o registro do empréstimo.");
        }
        int idOutro = idSolicitante == e.getIdProprietario() ? e.getIdLocatario() : e.getIdProprietario();
        Usuario outro = serviceUsuario.obterUsuario(idOutro);
        return new Contato(outro.getNome(), outro.getEmail(), outro.getTelefone());
    }

    @Override
    public Emprestimo obterEmprestimo(int idEmprestimo) {
        return emprestimos.obter(idEmprestimo);
    }

    @Override
    public List<Emprestimo> listarEmprestimosDoUsuario(int idUsuario) {
        return emprestimos.filtrar(e -> e.envolve(idUsuario));
    }

    @Override
    public List<Emprestimo> listarPorStatus(StatusEmprestimo status) {
        return emprestimos.filtrar(e -> e.getStatus() == status);
    }

    public List<String> getErrosEnvioEmail() {
        return List.copyOf(errosEnvioEmail);
    }

    // ------------------------------------------------------------------ auxiliares

    private Multa atualizarValorMulta(Emprestimo e) {
        Multa multa = serviceMultas.obterMultaPendente(e.getIdEmprestimo(), TipoMulta.DIARIA).orElse(null);
        if (multa != null) {
            double multaDiaria = RegrasNegocio.valorMultaDiaria(e.getValorDiaria());
            double novoValor = RegrasNegocio.arredondar(multaDiaria * Math.max(1, e.diasAtraso(hoje())));
            if (novoValor != multa.getValor()) {
                multa.setValor(novoValor);
                serviceMultas.editarMulta(multa.getIdMulta(), multa);
            }
        }
        return multa;
    }

    private void cancelar(Emprestimo e, String motivo) {
        e.alterarStatus(StatusEmprestimo.NEGOCIACAO_CANCELADA);
        emprestimos.salvar(e);
        if (e.getIdConversa() != 0) {
            servicoChat.encerrarConversa(e.getIdConversa(), motivo);
        }
        notificacoes.notificar(e.getIdLocatario(), motivo);
        notificacoes.notificar(e.getIdProprietario(), motivo);
    }

    private void verificarLimiteEmprestimos(Usuario usuario) {
        int limite = usuario.getLimiteEmprestimosSimultaneos();
        if (limite <= 0) {
            return;
        }
        long emCurso = emprestimos.filtrar(e -> e.getIdLocatario() == usuario.getIdUsuario()
                && e.getStatus().isEmprestimoEmCurso()).size();
        if (emCurso >= limite) {
            throw new RegraNegocioException("Conta com reputação baixa: limite de " + limite
                    + " empréstimo(s) simultâneo(s) atingido.");
        }
    }

    /** Falha no e-mail não pode desfazer a penalidade: registra o erro e avisa pela plataforma. */
    private void enviarEmailSeguro(String para, String assunto, String corpo) {
        try {
            servicoEmail.enviar(para, assunto, corpo);
        } catch (RuntimeException ex) {
            errosEnvioEmail.add(hoje() + " - falha ao enviar \"" + assunto + "\" para " + para + ": " + ex.getMessage());
        }
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
