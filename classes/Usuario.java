package classes;

/**
 * Usuário do sistema. Pode atuar como proprietário (dos jogos que cadastrou)
 * ou como interessado/locatário (nos jogos de outros).
 */
public class Usuario {
    private int idUsuario;
    private String nome;
    private final String cpf;
    private String apelido;
    private boolean ativo = true;

    /** 
     * E-mail e telefone são dados restritos (RNF01): só são expostos
     * pelo ServicoEmprestimos depois que o empréstimo é registrado.
    */
    private String email;
    private String senhaHash;
    private String telefone;
    private Endereco endereco;

    private StatusConta statusConta = StatusConta.ATIVA;
    private boolean reputacaoBaixa;
    /** 0 = sem limite. Após um extravio, a conta fica limitada a 1 empréstimo simultâneo (UC12 FA01). */
    private int limiteEmprestimosSimultaneos;
    /** Créditos recebidos como proprietário (repasse de multas e ressarcimentos). */
    private double saldoCredito;

    public Usuario(String nome, String cpf, String apelido, Endereco endereco) {
        this.nome = nome;
        this.cpf = cpf;
        this.apelido = apelido;
        this.endereco = endereco;
    }

    public boolean isAdministrador() {
        return false;
    }

    /** Bloqueado = não pode pegar jogos emprestados (multa pendente ou suspensão). */
    public boolean isBloqueado() {
        return statusConta != StatusConta.ATIVA;
    }

    public void creditar(double valor) {
        this.saldoCredito = RegrasNegocio.arredondar(saldoCredito + valor);
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public boolean getAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco novoEndereco) { this.endereco = novoEndereco; }
    public StatusConta getStatusConta() { return statusConta; }
    public void setStatusConta(StatusConta statusConta) { this.statusConta = statusConta; }
    public boolean isReputacaoBaixa() { return reputacaoBaixa; }
    public void setReputacaoBaixa(boolean reputacaoBaixa) { this.reputacaoBaixa = reputacaoBaixa; }
    public int getLimiteEmprestimosSimultaneos() { return limiteEmprestimosSimultaneos; }
    public void setLimiteEmprestimosSimultaneos(int limite) { this.limiteEmprestimosSimultaneos = limite; }
    public double getSaldoCredito() { return saldoCredito; }
}
