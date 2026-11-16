package emprestimojogos.service;

import emprestimojogos.classes.Endereco;
import emprestimojogos.classes.Usuario;

/** Serviço de Usuários (RF01, RF02, RF12, RF14; RNF01). */
public interface ServiceUsuario {
    // ---- operações do diagrama de classes ----
    void registrarUsuario(Usuario usuario);

    void desativarUsuario(int idUsuario);

    Usuario gerarUsuario(String nome, String cpf, String apelido, Endereco endereco);

    Endereco gerarEndereco(String pais, String uf, String logradouro, int numero, String complemento);

    Usuario buscarUsuarioNome(String nome);

    // ---- casos de uso ----
    /** UC01 – Cadastrar conta. */
    Usuario cadastrarConta(DadosCadastro dados);

    /** UC02 – Fazer login. */
    Usuario login(String email, String senha);

    Usuario obterUsuario(int idUsuario);

    PerfilPublico obterPerfilPublico(int idUsuario);

    // ---- status de bloqueio (usado pelo Serviço de Multas) ----
    void bloquear(int idUsuario);

    void suspender(int idUsuario);

    void desbloquear(int idUsuario);

    void creditar(int idUsuario, double valor);
}
