# EngSoft-emprestimo-jogos
Sistema de controle empréstimo de jogo, modelado e implementado na disciplina de Engenharia de Software I


## Como rodar

Requisitos: **Java 17+** e **Maven 3.9+**.

```bash
mvn compile exec:java    # demonstração de ponta a ponta no console
```

## Testes

Testes automatizados em JUnit 5, cobrindo os fluxos normais e alternativos (FA01–FA03) dos casos de uso.

```bash
mvn test                                        # todos os testes
mvn test -Dtest=ServiceUsuarioTest              # uma classe
mvn test -Dtest=ServiceUsuarioTest#cadastroValido   # um método
```

### Formato da saída

Cada classe aparece com o título do seu `@DisplayName`, e cada teste com o nome do seu `@DisplayName`, na ordem em que está no código. Teste que falha aparece com `✘` e o motivo logo abaixo.

```
$ mvn test -Dtest=ServiceUsuarioTest

▶ ServiceUsuarioTest  UC01 Cadastrar conta / UC02 Fazer login
  ✔ UC01: cria e persiste a conta com senha em hash
  ✔ UC01: dados inválidos ou incompletos indicam os campos a corrigir
  ✔ UC01: e-mail já cadastrado
  ...
  ✔ Bloqueio não rebaixa conta suspensa (UC11 FA02)
✔ 12 testes · 12 passaram · 0 falhas
```

Rodando todas as classes, cada uma aparece em sequência e, no fim, vem o total:

```
$ mvn test
...
✔ 21 testes · 21 passaram · 0 falhas

✔ 94 testes · 94 passaram · 0 falhas · 8 classes
```

### Testes por grupo

| Grupo | Classe | Casos de uso | Testes |
|-------|--------|--------------|-------:|
| 1. Usuários | `ServiceUsuarioTest` | UC01 Cadastrar conta, UC02 Fazer login | 12 |
| 2. Jogos e anúncios | `ServiceJogosTest` | UC03 Cadastrar jogo, UC05 Visualizar, UC06 Pesquisar | 14 |
| | `ServiceAnuncioTest` | UC04 Gerenciar anúncio | 11 |
| 3. Empréstimos | `ServicoEmprestimosTest` | UC07 Manifestar interesse, UC09 Registrar empréstimo, UC10 Confirmar devolução | 21 |
| 4. Chat | `ServicoChatTest` | UC08 Negociar pelo chat | 9 |
| 5. Penalidades e multas | `AplicarPenalidadeTest` | UC11 Aplicar penalidade | 8 |
| | `ServiceMultasTest` | UC12 Regularizar multa | 10 |
| Agendador | `AgendadorPrazosTest` | RF10–RF13 Agendador de prazos | 9 |
| **Total** | | | **94** |

### Como fica na pasta

```
src/test
├── java/emprestimojogos
│   ├── agendamento/
│   │   └── AgendadorPrazosTest.java
│   ├── apoio/
│   │   ├── Cenario.java          # monta usuários, jogos e empréstimos em qualquer estado
│   │   ├── OrdemDoCodigo.java    # roda os testes na ordem em que estão no código
│   │   └── RelatorioSlide.java   # imprime a saída no formato acima
│   └── service/
│       ├── AplicarPenalidadeTest.java
│       ├── ServiceAnuncioTest.java
│       ├── ServiceJogosTest.java
│       ├── ServiceMultasTest.java
│       ├── ServiceUsuarioTest.java
│       ├── ServicoChatTest.java
│       └── ServicoEmprestimosTest.java
└── resources/
    ├── junit-platform.properties # ativa o OrdemDoCodigo
    └── META-INF/services/
        └── org.junit.platform.launcher.TestExecutionListener   # ativa o RelatorioSlide

.mvn/
├── jvm.config                    # saída do Maven em UTF-8
└── maven.config                  # -q: esconde as linhas [INFO] do Maven
```

O `Cenario` usa um relógio ajustável para avançar os dias sem esperar dias reais, e dublês de e-mail e pagamento para simular falhas.

Para um teste novo aparecer bem na saída, coloque `@DisplayName` na classe e em cada método `@Test`. Sem ele, aparece o nome do método.

> **Acentos no Windows:** se o terminal mostrar `├ú` no lugar de `ã`, rode `chcp 65001` antes do `mvn test`.

## Estrutura (camadas do projeto arquitetural)

```
src/main/java/emprestimojogos
├── Main.java                    # demonstração de ponta a ponta no console (mvn compile exec:java)
├── SistemaEmprestimoJogos.java  # monta o sistema: liga repositórios, infra e serviços
├── agendamento/                 # AgendadorPrazos: verifica prazos, multas, extravio e chat inativo todo dia
├── classes/                     # entidades de domínio (Usuario, Jogo, Anuncio, Emprestimo, Multa, Conversa...),
│                                # enums de status e RegrasNegocio (parâmetros das regras)
├── excecao/                     # exceções do sistema (validação, acesso negado, regra de negócio...)
├── infra/                       # serviços auxiliares atrás de interfaces, com versões simuladas:
│                                # e-mail no console, gateway de pagamento, processamento de imagens,
│                                # valor de mercado, hash de senha, validação de CPF, gerador de códigos
│                                # e relógio ajustável
├── repository/                  # Repositorio<T> e a implementação em memória (RepositorioMemoria)
└── service/                     # interfaces dos serviços de negócio e objetos de dados (DadosCadastro, ItemCatalogo...)
    └── impl/                    # implementação de cada serviço (usuários, jogos, anúncios, empréstimos,
                                 # chat, multas e notificações)
```

## Regras de negócio e parâmetros (`classes/RegrasNegocio`)

| Regra | Valor | Origem |
|-------|-------|--------|
| Diária do empréstimo | 5% do valor do jogo | UC09 |
| Multa diária | diária + 10% da plataforma | UC11 (percentual da plataforma não definido no relatório) |
| Repasse ao dono | a diária (multa sem os 10%) / o valor do jogo (reposição) | UC11, UC12 FA01 |
| Tolerância para o dono confirmar | 2 dias após o prazo | UC11 ("prazo de tolerância") |
| Extravio | mais de 30 dias de atraso | UC11 FA01 ("ex.: 30-45 dias") |
| Reposição | valor do jogo + 10% de multa administrativa | UC11 FA01 |
| Inatividade do chat | aviso após 5 dias; encerra 2 dias depois | UC08 |
| Conta após ressarcimento | ativa, reputação baixa, limite de 1 empréstimo simultâneo | UC12 FA01 |
