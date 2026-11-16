package emprestimojogos.service;

/** O que os Controladores REST expõem de um usuário (RNF01): sem e-mail, telefone ou CPF. */
public record PerfilPublico(int idUsuario, String apelido, String uf, boolean reputacaoBaixa) {
}
