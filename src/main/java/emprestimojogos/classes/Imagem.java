package emprestimojogos.classes;

// Imagem de um anúncio (redimensionada e compactada).
public class Imagem {
    private int idImagem;
    private final String nome;
    private final int tamanho;
    private final String caminho;
    private final int largura;
    private final int altura;

    public Imagem(String nome, int tamanho, String caminho, int largura, int altura) {
        this.nome = nome;
        this.tamanho = tamanho;
        this.caminho = caminho;
        this.largura = largura;
        this.altura = altura;
    }

    public int getIdImagem() { return idImagem; }
    public void setIdImagem(int idImagem) { this.idImagem = idImagem; }
    public String getNome() { return nome; }
    public int getTamanho() { return tamanho; }
    public String getCaminho() { return caminho; }
    public int getLargura() { return largura; }
    public int getAltura() { return altura; }
}
