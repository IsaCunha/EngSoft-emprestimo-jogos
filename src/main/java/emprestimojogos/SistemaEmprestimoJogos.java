package emprestimojogos;

import emprestimojogos.agendamento.AgendadorPrazos;
import emprestimojogos.infra.ConsultaValorMercado;
import emprestimojogos.infra.GatewayPagamento;
import emprestimojogos.infra.GatewayPagamentoSimulado;
import emprestimojogos.infra.ProcessadorImagens;
import emprestimojogos.infra.ProcessadorImagensImageIO;
import emprestimojogos.infra.ServicoEmail;
import emprestimojogos.infra.ServicoEmailConsole;
import emprestimojogos.infra.TabelaValorMercado;
import emprestimojogos.repository.Repositorios;
import emprestimojogos.service.ServiceAnuncio;
import emprestimojogos.service.ServiceJogos;
import emprestimojogos.service.ServiceMultas;
import emprestimojogos.service.ServiceUsuario;
import emprestimojogos.service.ServicoChat;
import emprestimojogos.service.ServicoNotificacoes;
import emprestimojogos.service.impl.ServiceAnuncioImpl;
import emprestimojogos.service.impl.ServiceJogosImpl;
import emprestimojogos.service.impl.ServiceMultasImpl;
import emprestimojogos.service.impl.ServiceUsuarioImpl;
import emprestimojogos.service.impl.ServicoChatImpl;
import emprestimojogos.service.impl.ServicoEmprestimosImpl;
import emprestimojogos.service.impl.ServicoNotificacoesImpl;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Ponto de montagem do monólito modular: cria os repositórios e injeta as dependências
 * nos serviços (papel que o contêiner do Spring cumpriria).
 */
public class SistemaEmprestimoJogos {
    private final Repositorios repositorios;
    private final ServicoNotificacoes notificacoes;
    private final ServiceUsuario usuarios;
    private final ServiceAnuncio anuncios;
    private final ServiceJogos jogos;
    private final ServiceMultas multas;
    private final ServicoChat chat;
    private final ServicoEmprestimosImpl emprestimos;
    private final AgendadorPrazos agendador;

    public SistemaEmprestimoJogos(Repositorios repositorios, Clock clock, ServicoEmail email,
                                  GatewayPagamento gateway, ConsultaValorMercado valorMercado,
                                  ProcessadorImagens processadorImagens) {
        this.repositorios = repositorios;
        this.notificacoes = new ServicoNotificacoesImpl(repositorios.notificacoes(), clock);
        this.usuarios = new ServiceUsuarioImpl(repositorios.usuarios());
        this.anuncios = new ServiceAnuncioImpl(repositorios, processadorImagens, notificacoes);
        this.jogos = new ServiceJogosImpl(repositorios, usuarios, anuncios, valorMercado);
        this.multas = new ServiceMultasImpl(repositorios, usuarios, gateway, notificacoes, clock);
        this.chat = new ServicoChatImpl(repositorios, notificacoes, clock);
        this.emprestimos = new ServicoEmprestimosImpl(repositorios.emprestimos(), usuarios, jogos, multas, chat,
                email, notificacoes, clock);
        this.agendador = new AgendadorPrazos(emprestimos, multas, chat, clock);
    }

    /** Configuração padrão: repositórios em memória, e-mail no console e pagamento simulado. */
    public static SistemaEmprestimoJogos padrao(Clock clock, Path diretorioImagens) {
        return new SistemaEmprestimoJogos(Repositorios.emMemoria(), clock, new ServicoEmailConsole(),
                new GatewayPagamentoSimulado(), new TabelaValorMercado(),
                new ProcessadorImagensImageIO(diretorioImagens));
    }

    public Repositorios repositorios() { return repositorios; }
    public ServicoNotificacoes notificacoes() { return notificacoes; }
    public ServiceUsuario usuarios() { return usuarios; }
    public ServiceAnuncio anuncios() { return anuncios; }
    public ServiceJogos jogos() { return jogos; }
    public ServiceMultas multas() { return multas; }
    public ServicoChat chat() { return chat; }
    public ServicoEmprestimosImpl emprestimos() { return emprestimos; }
    public AgendadorPrazos agendador() { return agendador; }
}
