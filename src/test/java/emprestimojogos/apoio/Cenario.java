package emprestimojogos.apoio;

import emprestimojogos.SistemaEmprestimoJogos;
import emprestimojogos.classes.Emprestimo;
import emprestimojogos.classes.Endereco;
import emprestimojogos.classes.Jogo;
import emprestimojogos.classes.Usuario;
import emprestimojogos.infra.GatewayPagamento;
import emprestimojogos.infra.ProcessadorImagensImageIO;
import emprestimojogos.infra.RelogioAjustavel;
import emprestimojogos.infra.ResultadoPagamento;
import emprestimojogos.infra.ServicoEmail;
import emprestimojogos.infra.TabelaValorMercado;
import emprestimojogos.infra.ValidadorCpf;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.DadosCadastro;
import emprestimojogos.service.DadosJogo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cenário de teste (fixture): monta o sistema completo com repositórios em memória,
 * relógio ajustável e dublês de e-mail e pagamento, e oferece atalhos para levar um
 * empréstimo até cada estado do diagrama de estados.
 */
public class Cenario {
    public static final String SENHA = "senhaSegura1";
    public static final LocalDate DATA_INICIAL = LocalDate.of(2026, 11, 16);

    public final RelogioAjustavel relogio = RelogioAjustavel.em(DATA_INICIAL);
    public final EmailFalso email = new EmailFalso();
    public final GatewayFalso gateway = new GatewayFalso();
    public final TabelaValorMercado precos = new TabelaValorMercado();
    public final SistemaEmprestimoJogos sistema;

    private int sequenciaCpf = 100_000_000;

    public Cenario() {
        try {
            sistema = new SistemaEmprestimoJogos(Repositorios.emMemoria(), relogio, email, gateway, precos,
                    new ProcessadorImagensImageIO(Files.createTempDirectory("imagens-teste")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ tempo

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    /** Avança dia a dia, rodando o Agendador de Prazos a cada dia (como faria o job diário). */
    public void passarDias(int dias) {
        for (int i = 0; i < dias; i++) {
            relogio.avancarDias(1);
            sistema.agendador().executar();
        }
    }

    // ------------------------------------------------------------------ usuários e jogos

    public String proximoCpf() {
        return ValidadorCpf.completar(String.valueOf(sequenciaCpf++));
    }

    /** Dados válidos de cadastro; e-mail = nome em minúsculas + "@email.com", CPF sempre novo. */
    public DadosCadastro dadosCadastro(String nome) {
        Endereco endereco = new Endereco("Brasil", "MG", "Av. Antônio Carlos", 6627, null, "31270-901");
        return new DadosCadastro(nome, proximoCpf(), nome, nome.toLowerCase() + "@email.com", SENHA,
                "31999990000", endereco);
    }

    public Usuario novoUsuario(String nome) {
        return sistema.usuarios().cadastrarConta(dadosCadastro(nome));
    }

    public Usuario recarregar(Usuario usuario) {
        return sistema.usuarios().obterUsuario(usuario.getIdUsuario());
    }

    public Jogo novoJogo(Usuario dono, String nome, double valor) {
        return sistema.jogos().cadastrarJogo(dono.getIdUsuario(),
                DadosJogo.semImagem(nome, "Descrição de " + nome, 4, 60, "Boa", valor));
    }

    // ------------------------------------------------------------------ empréstimos (estados)

    /** SOLICITADO/EM_NEGOCIACAO: interessado manifesta interesse e a conversa é aberta. */
    public Emprestimo negociacao(Usuario interessado, Jogo jogo) {
        return sistema.emprestimos().manifestaInteresse(interessado.getIdUsuario(), jogo.getIdJogo());
    }

    /** AGUARDANDO_DEVOLUCAO: registrado pelo dono e retirado com o código. */
    public Emprestimo emprestimoRetirado(Usuario dono, Usuario locatario, Jogo jogo, int diasDePrazo) {
        Emprestimo e = negociacao(locatario, jogo);
        sistema.emprestimos().registrarEmprestimo(dono.getIdUsuario(), e.getIdEmprestimo(),
                locatario.getIdUsuario(), hoje().plusDays(diasDePrazo));
        String codigo = sistema.emprestimos().obterCodigoRetirada(dono.getIdUsuario(), e.getIdEmprestimo());
        sistema.emprestimos().confirmarRecebimento(locatario.getIdUsuario(), e.getIdEmprestimo(), codigo);
        return e;
    }

    /** COM_PENDENCIA: prazo de 7 dias vence e o dono informa que o jogo não foi devolvido. */
    public Emprestimo emprestimoAtrasado(Usuario dono, Usuario locatario, Jogo jogo) {
        Emprestimo e = emprestimoRetirado(dono, locatario, jogo, 7);
        relogio.avancarDias(7);
        sistema.emprestimos().informarNaoDevolucao(dono.getIdUsuario(), e.getIdEmprestimo());
        return e;
    }

    /** Devolução: o locatário passa o código e o dono confirma. */
    public void devolver(Usuario dono, Usuario locatario, Emprestimo e) {
        String codigo = sistema.emprestimos().obterCodigoDevolucao(locatario.getIdUsuario(), e.getIdEmprestimo());
        sistema.emprestimos().confirmarDevolucao(dono.getIdUsuario(), e.getIdEmprestimo(), codigo);
    }

    // ------------------------------------------------------------------ imagens

    public static byte[] imagemPng(int largura, int altura) {
        try {
            BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ dublês

    /** E-mail falso: guarda as mensagens em memória e pode simular falhas de envio. */
    public static class EmailFalso implements ServicoEmail {
        public record EmailEnviado(String destinatario, String assunto, String corpo) {
        }

        private final List<EmailEnviado> enviados = new ArrayList<>();
        private int falhasPendentes;

        @Override
        public void enviar(String destinatario, String assunto, String corpo) {
            if (falhasPendentes > 0) {
                falhasPendentes--;
                throw new IllegalStateException("Servidor SMTP indisponível (simulado)");
            }
            enviados.add(new EmailEnviado(destinatario, assunto, corpo));
        }

        public List<EmailEnviado> para(String destinatario) {
            return enviados.stream().filter(m -> m.destinatario().equalsIgnoreCase(destinatario)).toList();
        }

        public List<EmailEnviado> todos() {
            return List.copyOf(enviados);
        }

        public void falharProximos(int quantidade) {
            falhasPendentes = quantidade;
        }
    }

    /** Gateway de pagamento falso: registra as cobranças aprovadas e pode passar a recusar. */
    public static class GatewayFalso implements GatewayPagamento {
        private final List<Double> cobrancas = new ArrayList<>();
        private boolean recusar;

        @Override
        public ResultadoPagamento cobrar(int idUsuario, double valor) {
            if (recusar) {
                return ResultadoPagamento.recusado("Cartão recusado (simulado)");
            }
            cobrancas.add(valor);
            return ResultadoPagamento.aprovado(UUID.randomUUID().toString());
        }

        public void recusarPagamentos() {
            recusar = true;
        }

        public List<Double> cobrancas() {
            return List.copyOf(cobrancas);
        }
    }
}
