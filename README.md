# fiap-mecanica-gestao

Microsserviço de **Gestão (Cliente + Veículo)**, extraído do monólito `fiap-mecanica`
(strangler fig, passo 1: o monólito continua intacto).

- Java 25 + Spring Boot 4.0.5, mesma Clean Architecture do monólito (`core` / `infra`).
- Banco próprio MySQL `mecanica_gestao` (Flyway `V1` cliente, `V2` veículo — V5/V6 do monólito).
- Endpoints com os mesmos paths do monólito: `/cliente/**`, `/veiculo/**` (perfil `ADMINISTRADOR`).
- Swagger: `/gestao/swagger-ui` e `/gestao/v3/api-docs`.
- Segurança: **só valida** o JWT emitido pelo monólito (`/authenticate/login`) ou pela Lambda.
  Mesmo `JWT_SECRET` (Base64URL, HS256) e `iss=mecanica-fiap`. Sem tabela `users`: as
  authorities vêm da claim `roles` do token.

Mecânico e Atendente **ficam no monólito** (ligados a `users` via FK `user_id` e ao login).

## Comandos

```bash
JAVA_HOME=/home/arthur/.jdks/jdk-25 ./mvnw verify   # testes (Testcontainers: precisa de Docker)
docker compose up --build                          # app em :8081, MySQL em :3308
```

Para testar localmente, gere o token no monólito (`POST /authenticate/login` com o admin
`22255588846`) e use-o aqui — os dois precisam do mesmo `JWT_SECRET`.

## Deploy

- Mesmo cluster EKS e namespace `fiap-mecanica` do monólito. Service `gestao-service`
  (LoadBalancer) — internamente `http://gestao-service`.
- CD garante o repositório ECR `fiap-mecanica-gestao` via AWS CLI (provisório, sem Terraform).
- Schema `mecanica_gestao` na mesma instância RDS do monólito (`createDatabaseIfNotExist`)
  até existir um RDS próprio no `fiap-mecanica-infra-db`.
