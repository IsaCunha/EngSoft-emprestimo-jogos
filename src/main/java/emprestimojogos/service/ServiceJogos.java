package emprestimojogos.service;

import emprestimojogos.classes.Jogo;

import java.util.List;

/** Serviço de Jogos (RF03-RF06): cadastro, catálogo, busca e disponibilidade. */
public interface ServiceJogos {
    // ---- operações do diagrama de classes ----
    void registrarJogo(Jogo jogo);

    /** Marca o jogo como indisponível (chamado pelo Serviço de Empréstimos). */
    void registrarEmprestimo(int idUsuario, int idJogo);

    /** Marca o jogo como disponível novamente. */
    void registrarDevolucao(int idUsuario, int idJogo);

    Jogo procurarJogo(String nome);

    // ---- casos de uso ----
    /** UC03 – Cadastrar jogo (cria o jogo e publica o anúncio). */
    Jogo cadastrarJogo(int idDono, DadosJogo dados);

    /** UC04 – Editar dados do jogo. */
    Jogo editarJogo(int idDono, int idJogo, DadosJogo dados);

    /** UC05 – Visualizar jogos (exclui os do próprio usuário logado; idUsuarioLogado null = visitante). */
    List<ItemCatalogo> listarCatalogo(Integer idUsuarioLogado);

    /** UC06 – Pesquisar jogos (sem diferenciar maiúsculas e acentos). */
    List<ItemCatalogo> pesquisar(String termo, Integer idUsuarioLogado);

    List<Jogo> listarMeusJogos(int idDono);

    Jogo obterJogo(int idJogo);
}
