package emprestimojogos.service;

/** Dados de contato liberados somente após o registro do empréstimo (RNF01). */
public record Contato(String nome, String email, String telefone) {
}
