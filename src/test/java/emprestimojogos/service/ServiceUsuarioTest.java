package br.ufmg.emprestimojogos.service;

import br.ufmg.emprestimojogos.apoio.Cenario;
import br.ufmg.emprestimojogos.excecao.AcessoNegadoException;
import br.ufmg.emprestimojogos.excecao.CredenciaisInvalidasException;
import br.ufmg.emprestimojogos.excecao.NaoEncontradoException;
import br.ufmg.emprestimojogos.excecao.RegraNegocioException;
import br.ufmg.emprestimojogos.excecao.ValidacaoException;
import br.ufmg.emprestimojogos.model.Endereco;
import br.ufmg.emprestimojogos.model.StatusConta;
import br.ufmg.emprestimojogos.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UC01 Cadastrar conta / UC02 Fazer login")
class ServiceUsuarioTest {
    private Cenario cenario;
    private ServiceUsuario service;

    @BeforeEach
    void setUp() {
        cenario = new Cenario();
        service = cenario.sistema.usuarios();
    }

    @Test
    @DisplayName("UC01: cria e persiste a conta com senha em hash")
    void cadastroValido() {
        Usuario u = service.cadastrarConta(cenario.dadosCadastro("Ana"));
        assertTrue(u.getIdUsuario() > 0);
        assertEquals("ana@email.com", u.getEmail());
        assertNotEquals(Cenario.SENHA, u.getSenhaHash());
        assertEquals(StatusConta.ATIVA, u.getStatusConta());
        assertEquals(u, service.obterUsuario(u.getIdUsuario()));
    }

    @Test
    @DisplayName("UC01: dados inválidos ou incompletos indicam os campos a corrigir")
    void dadosInvalidos() {
        DadosCadastro dados = new DadosCadastro("", "123.456.789-00", null, "email-invalido", "123", null, null);
        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> service.cadastrarConta(dados));
        assertTrue(ex.getCampos().containsAll(java.util.List.of("nome", "cpf", "email", "senha")));
    }

    @Test
    @DisplayName("UC01: e-mail já cadastrado")
    void emailDuplicado() {
        service.cadastrarConta(cenario.dadosCadastro("Ana"));
        DadosCadastro outro = cenario.dadosCadastro("Ana"); // outro CPF, mesmo e-mail
        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.cadastrarConta(outro));
        assertTrue(ex.getMessage().contains("E-mail já cadastrado"));
    }

    @Test
    @DisplayName("UC01: CPF já cadastrado")
    void cpfDuplicado() {
        DadosCadastro dados = cenario.dadosCadastro("Ana");
        service.cadastrarConta(dados);
        DadosCadastro mesmoCpf = new DadosCadastro("Outra", dados.cpf(), "outra", "outra@email.com",
                Cenario.SENHA, null, null);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.cadastrarConta(mesmoCpf));
        assertEquals("CPF já cadastrado.", ex.getMessage());
    }

    @Test
    @DisplayName("UC01: CPF com conta bloqueada por multa não pode criar nova conta")
    void cpfComPendencia() {
        DadosCadastro dados = cenario.dadosCadastro("Ana");
        Usuario ana = service.cadastrarConta(dados);
        service.bloquear(ana.getIdUsuario());
        DadosCadastro novaConta = new DadosCadastro("Ana Nova", dados.cpf(), "ana2", "ana2@email.com",
                Cenario.SENHA, null, null);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> service.cadastrarConta(novaConta));
        assertTrue(ex.getMessage().contains("multa pendente"));
    }

    @Test
    @DisplayName("UC01: sem apelido, usa o primeiro nome")
    void apelidoPadrao() {
        DadosCadastro dados = new DadosCadastro("Carlos Eduardo", cenario.proximoCpf(), " ", "carlos@email.com",
                Cenario.SENHA, null, null);
        assertEquals("Carlos", service.cadastrarConta(dados).getApelido());
    }

    @Test
    @DisplayName("UC02: login com credenciais corretas (e-mail sem diferenciar maiúsculas)")
    void loginValido() {
        Usuario ana = service.cadastrarConta(cenario.dadosCadastro("Ana"));
        assertEquals(ana.getIdUsuario(), service.login(" ANA@email.com ", Cenario.SENHA).getIdUsuario());
    }

    @Test
    @DisplayName("UC02: credenciais incorretas")
    void loginInvalido() {
        service.cadastrarConta(cenario.dadosCadastro("Ana"));
        assertThrows(CredenciaisInvalidasException.class, () -> service.login("ana@email.com", "errada123"));
        assertThrows(CredenciaisInvalidasException.class, () -> service.login("ninguem@email.com", Cenario.SENHA));
    }

    @Test
    @DisplayName("Conta desativada pelo administrador não faz login")
    void contaDesativada() {
        Usuario ana = service.cadastrarConta(cenario.dadosCadastro("Ana"));
        service.desativarUsuario(ana.getIdUsuario());
        assertFalse(service.obterUsuario(ana.getIdUsuario()).getAtivo());
        assertThrows(AcessoNegadoException.class, () -> service.login("ana@email.com", Cenario.SENHA));
    }

    @Test
    @DisplayName("RNF01: perfil público não expõe contato")
    void perfilPublico() {
        Usuario ana = service.cadastrarConta(cenario.dadosCadastro("Ana"));
        PerfilPublico perfil = service.obterPerfilPublico(ana.getIdUsuario());
        assertEquals("Ana", perfil.apelido());
        assertEquals("MG", perfil.uf());
        assertFalse(perfil.toString().contains("@"));
    }

    @Test
    @DisplayName("Operações do diagrama: gerarEndereco, gerarUsuario, registrarUsuario e buscarUsuarioNome")
    void operacoesDoDiagrama() {
        Endereco endereco = service.gerarEndereco("Brasil", "MG", "Rua A", 10, "apto 1");
        Usuario u = service.gerarUsuario("Maria Clara", "529.982.247-25", "mari", endereco);
        assertEquals(0, u.getIdUsuario());
        service.registrarUsuario(u);
        assertTrue(u.getIdUsuario() > 0);
        assertEquals("52998224725", u.getCpf());
        assertEquals(u, service.buscarUsuarioNome("maria clara"));
        assertEquals(u, service.buscarUsuarioNome("MARI"));
        assertThrows(NaoEncontradoException.class, () -> service.buscarUsuarioNome("Fulano"));
    }

    @Test
    @DisplayName("Bloqueio não rebaixa conta suspensa (UC11 FA02)")
    void bloqueioMantemSuspensao() {
        Usuario ana = service.cadastrarConta(cenario.dadosCadastro("Ana"));
        service.suspender(ana.getIdUsuario());
        service.bloquear(ana.getIdUsuario());
        assertEquals(StatusConta.SUSPENSA, service.obterUsuario(ana.getIdUsuario()).getStatusConta());
    }
}
