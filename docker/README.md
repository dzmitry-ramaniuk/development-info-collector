# Docker

Docker упаковывает приложение и его пользовательское окружение в переносимый OCI-образ, а затем запускает из него изолированные процессы. Раздел ведёт от модели контейнера к воспроизводимой сборке, локальной композиции и production-hardening.

## Содержание

1. [Материалы](#материалы)
2. [Маршрут изучения](#маршрут-изучения)
3. [Связанные разделы](#связанные-разделы)

## Материалы

1. [Основы контейнеров](01-основы-контейнеров.md) — image и container, namespaces, cgroups и OCI Registry.
2. [Dockerfile и сборка образов](02-dockerfile-и-сборка-образов.md) — слои, cache, multi-stage build, `.dockerignore` и Spring Boot.
3. [Compose, сети и хранилища](03-compose-сети-и-хранилища.md) — DNS, порты, volumes, bind mounts, health checks и стек с PostgreSQL/Redis.
4. [Безопасность и production practices](04-безопасность-и-production-practices.md) — supply chain, SBOM, scanning, secrets и runtime-hardening.

## Маршрут изучения

1. Разберите границу между образом, контейнером и виртуальной машиной.
2. Соберите Spring Boot image и исследуйте повторное использование слоёв.
3. Запустите Compose-стек, проверьте DNS, readiness и сохранность данных.
4. Зафиксируйте зависимости и примените ограничения production-профиля.

## Связанные разделы

- [Kubernetes](../kubernetes/README.md) — оркестрация и probes после освоения контейнеров.
- [Testcontainers](../тестирование/02-testcontainers.md) — контейнеры как disposable test dependencies.
- [Тюнинг и мониторинг JVM](../java/06-jvm-tuning-monitoring.md) — heap, GC и диагностика под лимитами cgroups.
