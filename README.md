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

Cada teste aparece com o nome do seu `@DisplayName`, na ordem em que está no código:

```
▶ ServiceUsuarioTest  UC01 Cadastrar conta / UC02 Fazer login
  ✔ UC01: cria e persiste a conta com senha em hash
  ✔ UC01: dados inválidos ou incompletos indicam os campos a corrigir
  ...
✔ 12 testes · 12 passaram · 0 falhas
```

| Classe | Casos de uso |
|--------|--------------|
| `ServiceUsuarioTest` | UC01 Cadastrar conta, UC02 Fazer login |
| `ServiceJogosTest` | UC03 Cadastrar jogo, UC05 Visualizar, UC06 Pesquisar |
| `ServiceAnuncioTest` | UC04 Gerenciar anúncio |
| `ServicoEmprestimosTest` | UC07 Manifestar interesse, UC09 Registrar empréstimo, UC10 Confirmar devolução |
| `ServicoChatTest` | UC08 Negociar pelo chat |
| `AplicarPenalidadeTest` | UC11 Aplicar penalidade |
| `ServiceMultasTest` | UC12 Regularizar multa |
| `AgendadorPrazosTest` | RF10–RF13 Agendador de prazos |

```
src/test
├── java/emprestimojogos
│   ├── agendamento/   # testes do agendador de prazos
│   ├── apoio/         # Cenario (monta usuários, jogos e empréstimos em qualquer estado),
│   │                  # RelatorioSlide (saída dos testes) e OrdemDoCodigo (ordem de execução)
│   └── service/       # testes dos serviços de negócio
└── resources/         # ativa o RelatorioSlide e o OrdemDoCodigo no JUnit
```

O `Cenario` usa um relógio ajustável para avançar os dias sem esperar dias reais, e dublês de e-mail e pagamento para simular falhas.

> **Acentos no Windows:** se o terminal mostrar `├ú` no lugar de `ã`, rode `chcp 65001` antes do `mvn test`.

## Estrutura (camadas do projeto arquitetural)

```
src/main/java/emprestimojogos
├── agendamento/     # Agendador de prazos (simulação do tempo e automação de alertas)
├── classes/         # Entidades de domínio (Usuário, Jogo, Empréstimo, Multa, etc.)
├── excecao/         # Tratamento de exceções customizadas do sistema
├── infra/           # Serviços auxiliares (Gateway de pagamento, gerador de códigos, e-mail console)
├── repository/      # Camada de persistência em memória (RepositorioMemoria)
└── service/         # Lógica e regras de negócio da aplicação
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
