package emprestimojogos.classes;

import java.util.ArrayList;
import java.util.List;

/** Anúncio que divulga um jogo no catálogo. */
public class Anuncio {
    private int idAnuncio;
    private int idJogo;
    private int idDono;
    private boolean ativo = false;
    private String titulo;
    private String descricao;
    private final List<Integer> idsImagens = new ArrayList<>();

    public Anuncio(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }

    // Conecta o anúncio ao jogo que está sendo ofertado
    public void vincular(int idJogo, int idDono) {
        this.idJogo = idJogo;
        this.idDono = idDono;
    }

    public int getIdAnuncio() { return idAnuncio; }
    public void setIdAnuncio(int idAnuncio) { this.idAnuncio = idAnuncio; }
    public int getIdJogo() { return idJogo; }
    public int getIdDono() { return idDono; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean getAtivo() { return ativo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTitulo() { return titulo; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
    public List<Integer> getIdsImagens() { return idsImagens; }
}
