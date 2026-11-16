package classes;

// Localização do usuário 
public class Endereco {
    private int idEndereco;
    private String pais;
    private String uf;
    private String logradouro;
    private int numero;
    private String complemento;
    private String cep;

    public Endereco(String pais, String uf, String logradouro, int numero, String complemento, String cep) {
        this.pais = pais;
        this.uf = uf;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.cep = cep;
    }

    public int getIdEndereco() { return idEndereco; }
    public void setIdEndereco(int idEndereco) { this.idEndereco = idEndereco; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
}
