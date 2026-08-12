---
---

# Безопасность и production practices

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Минимальный и воспроизводимый образ](#минимальный-и-воспроизводимый-образ)
3. [SBOM и vulnerability scanning](#sbom-и-vulnerability-scanning)
4. [Secrets](#secrets)
5. [Runtime-hardening](#runtime-hardening)
6. [Ресурсы и эксплуатация JVM](#ресурсы-и-эксплуатация-jvm)
7. [Production checklist](#production-checklist)
8. [Практические упражнения](#практические-упражнения)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Проверено:** 4 августа 2026 года.
- **Целевые версии:** Docker Engine/BuildKit 28.x, OCI Image/Distribution 1.1, Java 21 LTS.
- **Статус примеров:** `current`; policy thresholds зависят от threat model организации.
- **Первичные источники:** [Docker build best practices](https://docs.docker.com/build/building/best-practices/), [Build secrets](https://docs.docker.com/build/building/secrets/), [Docker resource constraints](https://docs.docker.com/engine/containers/resource_constraints/), [Docker Scout SBOM](https://docs.docker.com/scout/how-tos/view-create-sboms/).

## Минимальный и воспроизводимый образ

Выбирайте минимальный **поддерживаемый** runtime image: без compiler, shell/package manager, если они не нужны. Distroless уменьшает число пакетов и интерактивных инструментов, Alpine — компактен, но musl и native dependencies могут отличаться от glibc; обычный slim/JRE часто проще диагностировать. Размер — не эквивалент безопасности: важны происхождение, патчи и содержимое.

Tag удобен людям, но mutable. Production input фиксируют digest:

```dockerfile
# Реальный digest берётся из доверенного registry и обновляется автоматизированным PR.
FROM eclipse-temurin:21-jre@sha256:<approved-digest>
```

**Pinning** обеспечивает одинаковые bytes и защищает от незаметного перемещения tag, но замораживает и уязвимости. Нужен бот/процесс обновления, повторный scan, тестирование и подпись/проверка provenance. Используйте multi-architecture digest осознанно и проверяйте платформу.

## SBOM и vulnerability scanning

**SBOM** — инвентаризация компонентов и версий (обычно SPDX или CycloneDX). Она ускоряет ответ «есть ли у нас уязвимый пакет?», но не доказывает отсутствие уязвимостей.

```bash
docker scout sbom example/orders:1.0 --format spdx > sbom.spdx
docker scout cves example/orders:1.0
```

Альтернативные scanners допустимы; храните SBOM/attestations рядом с release metadata. Gate учитывает severity, наличие исправления, exploitability и исключения со сроком — один CVE count даёт ложные приоритеты. Сканируйте OS packages **и** Maven/Gradle dependencies на build и регулярно после release: базы CVE обновляются без пересборки image.

## Secrets

Секреты нельзя запекать в Dockerfile, `ARG`, `ENV`, image layer, Git или Compose-файл. Даже удалённый следующей инструкцией файл остаётся в предыдущем layer.

```dockerfile
# syntax=docker/dockerfile:1
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    ./gradlew bootJar --no-daemon
```

```bash
docker build --secret id=maven_settings,src="$HOME/.m2/settings.xml" .
```

Runtime secret получает workload из secret manager как короткоживущий credential или read-only file. Environment variables часто видны в inspect, dumps и диагностике; если приложение поддерживает file-based secret, предпочитайте файл с минимальными permissions. Настройте rotation и отзыв.

## Runtime-hardening

Начальная политика для stateless Spring Boot container:

```yaml
services:
  app:
    image: registry.example/orders@sha256:<approved-digest>
    user: "10001:10001"
    read_only: true
    tmpfs:
      - /tmp:size=64m,mode=1777
    cap_drop: [ALL]
    security_opt:
      - no-new-privileges:true
    mem_limit: 512m
    cpus: 1.0
    pids_limit: 200
    stop_grace_period: 30s
```

- **Non-root** снижает impact компрометации; фиксированный numeric UID работает и без `/etc/passwd`.
- **Read-only root filesystem** блокирует постоянную запись; разрешайте только необходимые named volumes/tmpfs. Проверьте `/tmp`, logs и agents.
- **Drop capabilities** и `no-new-privileges` уменьшают полномочия; добавляйте capability точечно после теста.
- Не монтируйте Docker socket: это практически контроль над daemon/host.
- Используйте штатные seccomp/AppArmor/SELinux profiles и rootless/user namespaces там, где поддерживается.
- Отправляйте logs в stdout/stderr, не в writable layer.

## Ресурсы и эксплуатация JVM

Без memory limit контейнер может конкурировать за всю RAM host; при превышении hard limit cgroup возможен OOM kill без Java `OutOfMemoryError`. Ограничьте memory, CPU и PIDs, а requests/reservations задайте в оркестраторе. Бюджетируйте heap и non-heap вместе:

```text
container memory > heap + metaspace + code cache + direct buffers + thread stacks + native overhead
```

Начальная доля `-XX:MaxRAMPercentage=75.0` — не универсальная истина: измерьте RSS, `jcmd VM.native_memory`, число потоков, direct memory и поведение GC под реальным limit. Настройте health/readiness, graceful SIGTERM, timeouts, метрики throttling/OOM/restarts. Подробности — [тюнинг JVM](../java/06-jvm-tuning-monitoring.html); следующий уровень размещения и probes — [Kubernetes](../kubernetes/README.html).

## Production checklist

- [ ] Base image из доверенного registry, минимален и закреплён digest.
- [ ] Image подписан; provenance и SBOM сохранены и проверяются policy.
- [ ] OS и application dependencies просканированы; исключения имеют owner/expiry.
- [ ] Процесс non-root, capabilities сброшены, root filesystem read-only.
- [ ] Нет secrets в image/config history; настроена rotation.
- [ ] Заданы memory/CPU/PID limits и оставлен измеренный non-heap запас JVM.
- [ ] Readiness/liveness различены, graceful timeout согласован с платформой.
- [ ] Есть rollback, регулярная пересборка, backup/restore и observability.

## Практические упражнения

1. Сгенерируйте SBOM и найдите версии JRE и application dependencies.
2. Запустите image с `--read-only --tmpfs /tmp --cap-drop ALL` и устраните только необходимые записи.
3. Проверьте отсутствие секретов через `docker history --no-trunc` и `docker image inspect`.

## Вопросы для самопроверки

1. **Почему pinning по digest недостаточен?**  
   *Ответ:* Он фиксирует bytes, но требует обновления при исправлениях и проверки происхождения.
2. **Заменяет ли SBOM scanner?**  
   *Ответ:* Нет: SBOM инвентаризирует, scanner сопоставляет компоненты с постоянно меняющимися advisories.
3. **Зачем read-only filesystem вместе с non-root?**  
   *Ответ:* Меры ограничивают разные возможности атакующего и реализуют defense-in-depth.
