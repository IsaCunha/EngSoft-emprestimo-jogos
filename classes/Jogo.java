package classes;

/** Jogo físico (tabuleiro ou cartas) cadastrado por um usuário. */
public class Jogo {
    private int idJogo;
    private final int idProprietario;
    private String nome;
    private String descricao;
    private int numeroJogadores;
    private int duracaoMediaMinutos;
    private String condicao;
    private double valorEstimado;
    private boolean disponivel = true;

    public Jogo(int idProprietario, String nome, String descricao, int numeroJogadores,
                int duracaoMediaMinutos, String condicao, double valorEstimado) {
        this.idProprietario = idProprietario;
        this.nome = nome;
        this.descricao = descricao;
        this.numeroJogadores = numeroJogadores;
        this.duracaoMediaMinutos = duracaoMediaMinutos;
        this.condicao = condicao;
        this.valorEstimado = valorEstimado;
    }

    /** Usado no diagrama de sequência: ServicoEmprestimos -> jogo.verificarDisponibilidade(). */
    public boolean verificarDisponibilidade() {
        return disponivel;
    }

    public int getIdJogo() { return idJogo; }
    public void setIdJogo(int idJogo) { this.idJogo = idJogo; }
    public int getIdProprietario() { return idProprietario; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getNumeroJogadores() { return numeroJogadores; }
    public void setNumeroJogadores(int numeroJogadores) { this.numeroJogadores = numeroJogadores; }
    public int getDuracaoMediaMinutos() { return duracaoMediaMinutos; }
    public void setDuracaoMediaMinutos(int duracao) { this.duracaoMediaMinutos = duracao; }
    public String getCondicao() { return condicao; }
    public void setCondicao(String condicao) { this.condicao = condicao; }
    public double getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(double valorEstimado) { this.valorEstimado = valorEstimado; }
    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
}
