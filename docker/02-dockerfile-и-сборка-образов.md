---
---

# Dockerfile и сборка образов

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Слои и build cache](#слои-и-build-cache)
3. [Multi-stage build](#multi-stage-build)
4. [Dockerignore](#dockerignore)
5. [Spring Boot: production-ready образ](#spring-boot-production-ready-образ)
6. [Проблемы и советы](#проблемы-и-советы)
7. [Практические упражнения](#практические-упражнения)
8. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Проверено:** 4 августа 2026 года.
- **Целевые версии:** Docker Engine/BuildKit 28.x, Java 21 LTS, Spring Boot 3.5.x.
- **Статус примеров:** `current`.
- **Первичные источники:** [Docker build cache](https://docs.docker.com/build/cache/), [multi-stage builds](https://docs.docker.com/build/building/multi-stage/), [Spring Boot container images](https://docs.spring.io/spring-boot/how-to/packaging.html#howto.packaging.container-images).

## Слои и build cache

Инструкции Dockerfile формируют build graph и, для filesystem-операций, immutable layers. Cache key зависит от инструкции, входных файлов, build args и родительского результата. Изменение раннего шага инвалидирует зависимые последующие шаги, поэтому стабильные зависимости копируют раньше часто меняющегося source.

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon
```

Cache mount ускоряет загрузки, но не попадает в итоговый layer. Не объединяйте всё в один `COPY . .`: изменение README тогда зря сбросит cache. Секрет не передают через `ARG`/`ENV`, поскольку metadata и layers могут его сохранить; используйте BuildKit secret mount.

## Multi-stage build

**Multi-stage build** отделяет toolchain от runtime. `COPY --from=build` переносит только artifact: JDK, Gradle cache и source не попадают в final image. Это уменьшает поверхность атаки и размер, но не заменяет scanning.

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 app && \
    useradd --system --uid 10001 --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-XX:InitialRAMPercentage=25.0","-jar","/app/app.jar"]
```

Современная Java учитывает cgroup CPU/memory при ergonomic-настройках (`UseContainerSupport` включён по умолчанию). Явный `MaxRAMPercentage` задаёт бюджет heap внутри container limit, но оставляет запас для metaspace, code cache, thread stacks, direct buffers и native libraries. Не передавайте `-Xmx`, вычисленный из RAM host, и не отключайте container support. Проверяйте решение через `java -XshowSettings:system -version` и Native Memory Tracking; подробнее — [JVM tuning](../java/06-jvm-tuning-monitoring.html).

## Dockerignore

`.dockerignore` исключает файлы из build context до отправки builder: меньше I/O, меньше случайных cache misses и риска скопировать секреты.

```gitignore
.git
.gradle
build
.idea
*.iml
.env
**/*.pem
```

Это defense-in-depth, а не средство отзыва уже попавшего секрета. Нужные wrapper-файлы исключать нельзя.

## Spring Boot: production-ready образ

Приложение должно завершаться по `SIGTERM`. Exec-form `ENTRYPOINT` делает Java PID 1 без промежуточного shell. Spring Boot выполняет graceful shutdown в пределах lifecycle timeout; внешний stop timeout должен быть больше него.

```yaml
# application.yml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

```bash
docker build --pull -t example/orders:dev .
docker run --rm --memory=512m --cpus=1 -p 8080:8080 example/orders:dev
# В другом терминале: curl localhost:8080/actuator/health; docker stop --time 30 <id>
```

В production закрепите `FROM` по проверенному digest и автоматизируйте обновления: digest обеспечивает воспроизводимость, но сам не приносит security fixes.

## Проблемы и советы

- Запускайте сборку с lockfiles/checksums и отдельным dependency layer.
- Не ставьте compiler/package manager в runtime stage.
- Не используйте shell-form `ENTRYPOINT java ...`: сигналы могут не дойти до JVM.
- `HEALTHCHECK` проверяет состояние, но сам не задаёт политику оркестрации и не заменяет readiness.

## Практические упражнения

1. Измените только Java source и сравните `CACHED` в `docker build --progress=plain`.
2. Сравните размеры build и runtime stages.
3. Остановите контейнер во время запроса и проверьте graceful shutdown по логам.

## Вопросы для самопроверки

1. **Почему порядок инструкций влияет на скорость?**  
   *Ответ:* Изменение cache key раннего узла пересобирает зависимые узлы.
2. **Что даёт multi-stage build?**  
   *Ответ:* Build dependencies остаются в промежуточной стадии, runtime получает только нужный artifact.
3. **Почему heap не должен занимать 100% memory limit?**  
   *Ответ:* JVM и приложение используют также native memory, stacks, metaspace и buffers.
