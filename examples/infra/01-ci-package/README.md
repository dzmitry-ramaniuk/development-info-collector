---
---

# Проверенный JAR → runtime image

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Контракт примера](#контракт-примера)
3. [Локальный запуск](#локальный-запуск)
4. [Публикация и digest](#публикация-и-digest)
5. [Упражнение и самопроверка](#упражнение-и-самопроверка)

## Актуальность материала

- **Проверка документации:** 2026-10-15.
- **Baseline:** Java 21, Maven 3.9.x; версии compiler/Surefire/JAR plugins и JUnit зафиксированы в `pom.xml`. Docker Engine 28.x с BuildKit; публикация дополнительно требует Docker Scout plugin и его credentials.
- **Статус:** `current` для учебного package/integrity workflow; cloud deployment отсутствует. CVE scanner и публикация запускаются отдельно, их успех здесь не заявляется.
- **Первичные источники:** [Maven reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html), [Docker build](https://docs.docker.com/reference/cli/docker/buildx/build/), [Scout CVE exit code](https://docs.docker.com/reference/cli/docker/scout/cves/), [GitHub artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data).

## Контракт примера

Приложение считает стоимость заказа в целых minor units: `1200 × 3 = 3600`; неположительное количество и переполнение завершают CLI с кодом 2. Maven запускает шесть проверок бизнес-границ и создаёт **один** JAR. Следующий job получает этот файл с SHA-256; runtime-only Dockerfile копирует готовый JAR, не выполняя Maven и не получая source.

`02-image-gate.sh` проверяет checksum, разрешает runtime tag в digest, собирает image, извлекает JAR обратно и сравнивает bytes, проверяет non-root UID и успешный/ошибочный запрос при read-only filesystem. Любая ошибка останавливает скрипт до release evidence. Это integrity/runtime gate: он **не проверяет CVE**. Поле `vulnerabilityGate: not-run` сохраняет это различие.

[Workflow](../../../.github/workflows/infra-example.yml) запускается вручную: verify → upload JAR → download в новый job → build/gate → evidence. Он не имеет publish credentials. Action refs закреплены commit SHA. Maven на hosted runner — предварительно установленный 3.9.x; для строгой воспроизводимости toolchain закрепляют дополнительно. Timestamp JAR фиксирован; это помогает сравнить clean builds, но не доказывает воспроизводимость всего окружения.

## Локальный запуск

Из корня репозитория нужны JDK 21, Maven 3.9.x, Docker с доступным daemon, Bash, Python 3 и `sha256sum`:

```bash
bash examples/infra/01-ci-package/01-package.sh
java -jar examples/infra/01-ci-package/target/release/app.jar 1200 3
# 3600
bash examples/infra/01-ci-package/02-image-gate.sh infra-order-total:local
cat examples/infra/01-ci-package/target/release/image-evidence.json
python3 examples/infra/01-ci-package/04-test-publish-gate.py
```

`MVN_BIN` позволяет выбрать Maven; аргументы после `01-package.sh` передаются ему, например `-s /path/settings.xml`. Скрипт image gate выполняет pull runtime и локальный запуск контейнера; наружу он ничего не публикует. Для заранее утверждённой базы задайте `RUNTIME_IMAGE=repository@sha256:<64 hex>`. Настройки proxy/CA daemon и build environment должны доверять вашему registry без отключения TLS verification.

Негативная проверка: измените копию `target/release/app.jar`, сохранив старый checksum. `02-image-gate.sh` должен завершиться на `sha256sum -c` **до docker build** и не оставить `image-evidence.json`. Затем повторите package. Для очистки удалите только image из этого опыта (`docker image rm infra-order-total:local`) и каталог `target`; shared base image удалять не требуется.

## Публикация и digest

Публикация — отдельное явное действие с уже выполненными `docker login`, настроенным Docker Scout и правами на **ваш** repository:

```bash
# Замените repository на свой. Эти команды публикуют image только при ручном запуске.
bash examples/infra/01-ci-package/02-image-gate.sh ghcr.io/YOUR_ACCOUNT/order-total:reviewed
bash examples/infra/01-ci-package/03-publish.sh ghcr.io/YOUR_ACCOUNT/order-total:reviewed
```

Три offline-теста `04-test-publish-gate.py` используют fake Docker и доказывают отсутствие push при отказе scanner, смене tag и неподходящем evidence. Реальный registry они не вызывают.

`03-publish.sh` требует integrity evidence для того же image ID, запускает `docker scout cves --exit-code --only-severity critical,high`; находки и ошибка scanner блокируют push. После push он сохраняет **registry manifest digest**. До публикации доступен только `localImageId` — digest конфигурации image, его нельзя объявлять deployment digest registry. Репозиториям/окружениям передают `registryDigest`, не пересобирают image. Gate — учебная policy без исключений; перед production добавляют provenance/signatures/SBOM, проверку issuer/workflow identity и retention.

## Упражнение и самопроверка

1. Измените исходники после package: image job всё равно должен использовать ранее проверенный JAR. Пересборка приложения допускается только в новой verify-stage.
2. Переназначьте tag другому image после gate: publish должен остановиться до scanner/push.
3. Смоделируйте отказ scanner: ни push, ни статус `vulnerabilityGate: passed` не должны появиться.

**Почему два digest?** JAR checksum фиксирует приложение, registry digest — runtime filesystem/config вместе с приложением. **Почему одного CVE gate мало?** Он не доказывает качество кода и происхождение сборки.
