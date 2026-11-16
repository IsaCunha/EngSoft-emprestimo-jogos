package classes;

// Especialização de Usuario: permissões administrativas (ex.: desativar contas).
public class Administrador extends Usuario {
    public Administrador(String nome, String cpf, String apelido, Endereco endereco) {
        super(nome, cpf, apelido, endereco);
    }

    @Override
    public boolean isAdministrador() {
        return true;
    }
}
