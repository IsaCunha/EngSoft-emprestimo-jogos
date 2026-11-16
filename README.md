# EngSoft-emprestimo-jogos
Sistema de controle empréstimo de jogo, modelado e implementado na disciplina de Engenharia de Software I


## Como rodar

```bash
mvn compile exec:java    # demonstração de ponta a ponta no console
```

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
