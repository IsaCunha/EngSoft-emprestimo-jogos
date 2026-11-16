package emprestimojogos.service;

import emprestimojogos.classes.Endereco;

/** Dados informados na tela de cadastro (UC01). */
public record DadosCadastro(String nome, String cpf, String apelido, String email, String senha,
                            String telefone, Endereco endereco) {
}
