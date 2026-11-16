package emprestimojogos.service.impl;

import emprestimojogos.excecao.AcessoNegadoException;
import emprestimojogos.excecao.CredenciaisInvalidasException;
import emprestimojogos.excecao.NaoEncontradoException;
import emprestimojogos.excecao.RegraNegocioException;
import emprestimojogos.excecao.ValidacaoException;
import emprestimojogos.infra.HashSenha;
import emprestimojogos.infra.Texto;
import emprestimojogos.infra.ValidadorCpf;
import emprestimojogos.classes.Endereco;
import emprestimojogos.classes.RegrasNegocio;
import emprestimojogos.classes.StatusConta;
import emprestimojogos.classes.Usuario;
import emprestimojogos.repository.Repositorio;
import emprestimojogos.service.DadosCadastro;
import emprestimojogos.service.PerfilPublico;
import emprestimojogos.service.ServiceUsuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ServiceUsuarioImpl implements ServiceUsuario {
    public static final int TAMANHO_MINIMO_SENHA = 8;

    private final Repositorio<Usuario> usuarios;

    public ServiceUsuarioImpl(Repositorio<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public void registrarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não informado.");
        }
        usuarios.salvar(usuario);
    }

    @Override
    public void desativarUsuario(int idUsuario) {
        Usuario usuario = obterUsuario(idUsuario);
        usuario.setAtivo(false);
        usuarios.salvar(usuario);
    }

    @Override
    public Usuario gerarUsuario(String nome, String cpf, String apelido, Endereco endereco) {
        return new Usuario(nome, ValidadorCpf.somenteDigitos(cpf), apelido, endereco);
    }

    @Override
    public Endereco gerarEndereco(String pais, String uf, String logradouro, int numero, String complemento) {
        return new Endereco(pais, uf, logradouro, numero, complemento, null);
    }

    @Override
    public Usuario buscarUsuarioNome(String nome) {
        String alvo = Texto.normalizar(nome);
        return usuarios.filtrar(u -> Texto.normalizar(u.getNome()).equals(alvo)
                        || Texto.normalizar(u.getApelido()).equals(alvo))
                .stream().findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Nenhum usuário com o nome \"" + nome + "\"."));
    }

    @Override
    public Usuario cadastrarConta(DadosCadastro dados) {
        List<String> erros = new ArrayList<>();
        if (Texto.vazio(dados.nome())) {
            erros.add("nome");
        }
        if (!ValidadorCpf.valido(dados.cpf())) {
            erros.add("cpf");
        }
        if (!Texto.emailValido(dados.email())) {
            erros.add("email");
        }
        if (dados.senha() == null || dados.senha().length() < TAMANHO_MINIMO_SENHA) {
            erros.add("senha");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException("Dados inválidos ou incompletos.", erros);
        }

        String cpf = ValidadorCpf.somenteDigitos(dados.cpf());
        Optional<Usuario> mesmoCpf = usuarios.filtrar(u -> u.getCpf().equals(cpf)).stream().findFirst();
        if (mesmoCpf.isPresent()) {
            if (mesmoCpf.get().isBloqueado()) {
                throw new RegraNegocioException("Já existe uma conta com este CPF que possui multa pendente. "
                        + "Regularize a pendência na conta original.");
            }
            throw new RegraNegocioException("CPF já cadastrado.");
        }

        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        if (buscarPorEmail(email).isPresent()) {
            throw new RegraNegocioException("E-mail já cadastrado. Informe outro e-mail.");
        }

        String apelido = Texto.vazio(dados.apelido()) ? dados.nome().trim().split("\\s+")[0] : dados.apelido().trim();
        Usuario usuario = new Usuario(dados.nome().trim(), cpf, apelido, dados.endereco());
        usuario.setEmail(email);
        usuario.setSenhaHash(HashSenha.gerar(dados.senha()));
        usuario.setTelefone(dados.telefone());
        return usuarios.salvar(usuario);
    }

    @Override
    public Usuario login(String email, String senha) {
        Usuario usuario = buscarPorEmail(email == null ? "" : email.trim().toLowerCase(Locale.ROOT))
                .filter(u -> HashSenha.verificar(senha, u.getSenhaHash()))
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha incorretos."));
        if (!usuario.getAtivo()) {
            throw new AcessoNegadoException("Conta desativada.");
        }
        return usuario;
    }

    @Override
    public Usuario obterUsuario(int idUsuario) {
        return usuarios.obter(idUsuario);
    }

    @Override
    public PerfilPublico obterPerfilPublico(int idUsuario) {
        Usuario u = obterUsuario(idUsuario);
        String uf = u.getEndereco() == null ? null : u.getEndereco().getUf();
        return new PerfilPublico(u.getIdUsuario(), u.getApelido(), uf, u.isReputacaoBaixa());
    }

    @Override
    public void bloquear(int idUsuario) {
        Usuario u = obterUsuario(idUsuario);
        if (u.getStatusConta() == StatusConta.ATIVA) { // FA02: se já bloqueado/suspenso, mantém
            u.setStatusConta(StatusConta.BLOQUEADA);
            usuarios.salvar(u);
        }
    }

    @Override
    public void suspender(int idUsuario) {
        Usuario u = obterUsuario(idUsuario);
        u.setStatusConta(StatusConta.SUSPENSA);
        // a conta, quando restaurada, fica sob aviso de reputação baixa e limitada (UC12 FA01)
        u.setReputacaoBaixa(true);
        u.setLimiteEmprestimosSimultaneos(RegrasNegocio.LIMITE_EMPRESTIMOS_REPUTACAO_BAIXA);
        usuarios.salvar(u);
    }

    @Override
    public void desbloquear(int idUsuario) {
        Usuario u = obterUsuario(idUsuario);
        u.setStatusConta(StatusConta.ATIVA);
        usuarios.salvar(u);
    }

    @Override
    public void creditar(int idUsuario, double valor) {
        Usuario u = obterUsuario(idUsuario);
        u.creditar(valor);
        usuarios.salvar(u);
    }

    private Optional<Usuario> buscarPorEmail(String emailNormalizado) {
        return usuarios.filtrar(u -> emailNormalizado.equals(u.getEmail())).stream().findFirst();
    }
}
