# Docker Compose: сети и хранилища

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Networking](#networking)
3. [Bind mounts и volumes](#bind-mounts-и-volumes)
4. [Health checks и зависимости](#health-checks-и-зависимости)
5. [Spring Boot, PostgreSQL и Redis](#spring-boot-postgresql-и-redis)
6. [Проблемы и советы](#проблемы-и-советы)
7. [Практические упражнения](#практические-упражнения)
8. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Проверено:** 4 августа 2026 года.
- **Целевые версии:** Docker Engine 28.x, Docker Compose Specification/Compose v2, PostgreSQL 17, Redis 8.
- **Статус примеров:** `current` для локальной разработки; production требует внешнего secret manager и управляемых data services либо отдельного operational design.
- **Первичные источники:** [Compose networking](https://docs.docker.com/compose/how-tos/networking/), [Docker volumes](https://docs.docker.com/engine/storage/volumes/), [Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/).

## Networking

Compose создаёт default bridge network. Сервисы обнаруживаются встроенным DNS по **имени сервиса**, поэтому приложение обращается к `postgres:5432`, не к `localhost`: внутри app-контейнера `localhost` означает сам app-контейнер.

`ports: "8080:8080"` публикует container port на host. Для общения сервисов публикация не нужна: достаточно общей сети и container port. `expose` документирует внутренний порт, но не является firewall. Разные networks помогают ограничить достижимость, однако правила доступа и TLS всё равно нужны.

## Bind mounts и volumes

| Механизм | Источник | Лучше подходит | Компромисс |
|---|---|---|---|
| Bind mount | конкретный host path | source/config в dev | зависит от host, может менять host files |
| Named volume | управляет Docker | данные PostgreSQL | удобен перенос между containers, нужен backup |
| tmpfs | память host | временные чувствительные данные | исчезает после остановки |

Volume живёт независимо от container lifecycle. Он не заменяет backup: ошибочное удаление или corruption также сохраняются. Bind mount тесно связывает deployment с layout и permissions host; для config задавайте `read_only`.

```bash
docker volume inspect docker_pgdata
docker compose down       # named volume остаётся
docker compose down -v    # удаляет и данные: осторожно
```

## Health checks и зависимости

Running не означает ready. `healthcheck` запускает probe внутри container. `depends_on: condition: service_healthy` задерживает старт зависимого сервиса, но не делает приложение устойчивым к последующему падению зависимости: нужны timeouts, retry/backoff и восстановление соединений.

Проверка должна быть дешёвой и отражать готовность обслуживать запрос. Для Spring Boot используйте `/actuator/health/readiness`; для Docker image потребуется HTTP client либо отдельная JVM/Java probe. Не проверяйте PostgreSQL простым открытием TCP, если нужна готовность принимать запросы.

## Spring Boot, PostgreSQL и Redis

Пример использует образ из [предыдущей главы](02-dockerfile-и-сборка-образов.md), непривилегированный UID, cgroup limits и graceful stop. Пароли ниже предназначены **только для локальной разработки**.

```yaml
# compose.yaml
services:
  app:
    build: .
    init: true
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/app
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: local-only
      SPRING_DATA_REDIS_HOST: redis
      JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    stop_grace_period: 30s
    mem_limit: 512m
    cpus: 1.0
    pids_limit: 200
    restart: unless-stopped
    networks: [backend]

  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: local-only
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      timeout: 3s
      retries: 12
      start_period: 10s
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks: [backend]

  redis:
    image: redis:8-alpine
    command: ["redis-server", "--appendonly", "yes"]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 12
    volumes:
      - redisdata:/data
    networks: [backend]

volumes:
  pgdata:
  redisdata:

networks:
  backend:
    internal: true
```

`internal: true` изолирует сеть от внешней связности. Если сборке или app нужен outbound Internet, уберите параметр или добавьте app во вторую сеть. В production замените tags на утверждённые digests, passwords — secret injection, а Compose resource semantics проверьте для выбранного runtime.

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f app
curl --fail http://localhost:8080/actuator/health/readiness
docker compose stop app
docker compose down
```

Для integration tests тот же класс зависимостей удобнее поднимать программно через [Testcontainers](../тестирование/02-testcontainers.md), а не делить один Compose-стек между тестами.

## Проблемы и советы

- Не задавайте `container_name`: это мешает масштабированию и обычно не нужно для DNS.
- Не используйте host port базы, если доступ нужен только app.
- Добавьте миграции схемы и backup/restore drill; volume — не backup.
- Различайте liveness (процесс следует перезапустить) и readiness (временно не направлять traffic).

## Практические упражнения

1. Удалите app-контейнер и убедитесь, что данные PostgreSQL сохранились.
2. Выполните `docker compose exec app getent hosts postgres` и объясните DNS name.
3. Остановите Redis после старта и проверьте retry/recovery приложения.

## Вопросы для самопроверки

1. **Почему `localhost:5432` неверен из app?**  
   *Ответ:* Loopback принадлежит network namespace app; PostgreSQL доступен по service DNS name.
2. **Чем volume отличается от bind mount?**  
   *Ответ:* Lifecycle/path volume управляет Docker, bind mount напрямую отображает host path.
3. **Гарантирует ли `depends_on` доступность навсегда?**  
   *Ответ:* Нет, он упорядочивает старт; runtime failures обрабатывает приложение/оркестратор.
