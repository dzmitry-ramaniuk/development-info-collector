---
---

# Артефакты и promotion между средами

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Что является release artifact](#что-является-release-artifact)
3. [Immutable artifacts](#immutable-artifacts)
4. [Build once, promote many](#build-once-promote-many)
5. [Metadata, provenance и SBOM](#metadata-provenance-и-sbom)
6. [Promotion и среды](#promotion-и-среды)
7. [Audit trail](#audit-trail)
8. [Типичные ошибки](#типичные-ошибки)
9. [Практические упражнения](#практические-упражнения)
10. [Вопросы для самопроверки](#вопросы-для-самопроверки)
11. [Связанные темы](#связанные-темы)

## Актуальность материала

- **Проверено:** 4 августа 2026 года.
- **Целевые стандарты:** OCI Image Specification 1.1; SLSA 1.2; CycloneDX 1.6 или SPDX 3.0 для SBOM.
- **Статус примеров:** `current`; концепция применима к JAR, OCI image, Helm chart и serverless bundle.
- **Первичные источники:** [OCI Image Specification](https://specs.opencontainers.org/image-spec/), [SLSA 1.2](https://slsa.dev/spec/v1.2/), [CycloneDX specification](https://cyclonedx.org/specification/overview/), [SPDX specifications](https://spdx.dev/use/specifications/), [AWS ECR image tag immutability](https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-tag-mutability.html).

## Что является release artifact

**Release artifact** — развертываемый объект, созданный доверенной сборкой: JAR, OCI image, chart или архив функции. Source commit, CI workspace, cache и mutable tag сами по себе артефактом релиза не являются.

Минимальная release record связывает:

```text
commit SHA → build run → artifact digest → SBOM/provenance
           → approvals → environment/deployment → runtime version
```

## Immutable artifacts

**Immutable artifact** после публикации не изменяет байты по тому же идентификатору. Content digest (`sha256:...`) проверяет идентичность; tag `1.8.0` или `prod` — лишь удобный указатель и может быть mutable, если registry policy этого не запрещает.

Правила:

- запретить overwrite/delete release versions обычным CI identity;
- хранить checksum/digest вне job log, в release metadata;
- подписывать/аттестовать digest, а не tag;
- задавать retention отдельно для ephemeral PR artifacts и долгоживущих releases;
- не запекать environment secrets и URLs в бинарник: передавать конфигурацию при deployment.

Иммутабельность не означает вечное хранение. Retention policy может удалить объект контролируемо, сохранив audit record; после удаления быстрый rollback на него невозможен.

## Build once, promote many

**Build once, promote many** означает, что dev, staging и production получают одни и те же байты/digest. Promotion меняет статус/ссылку и доказательства допуска, но не запускает компиляцию или `docker build` заново.

```mermaid
flowchart LR
  B[Доверенная сборка] --> Q[Quarantine]
  Q -->|scan + tests| D[Допущен в dev]
  D -->|integration| S[Допущен в staging]
  S -->|approval + SLO| P[Допущен в production]
  P --> R[Runtime по digest]
```

Варианты promotion:

- один registry/repository: среда хранит desired digest;
- repositories по trust zones: server-side copy сохраняет manifest/layers и digest;
- release manifest: GitOps commit связывает digest образа и environment config.

Если копирование меняет media types или пересобирает manifest, проверьте итоговый digest и сохраните связь исходного и целевого identities.

## Metadata, provenance и SBOM

| Доказательство | Отвечает на вопрос |
|---|---|
| Digest/checksum | Это те же байты? |
| Provenance/attestation | Кто, где и из каких inputs собрал? |
| SBOM | Какие компоненты входят? |
| Signature | Какая identity подтверждает digest/attestation? |
| Scan report | Что scanner/policy обнаружили в момент проверки? |

SBOM не доказывает безопасность, signature не доказывает качество, а provenance не заменяет тесты. Проверяйте все доказательства policy engine перед promotion. Attestation должна быть проверяема независимо: доверяйте issuer/workflow identity, audience и source ref, а не только наличию файла.

## Promotion и среды

Среда состоит из артефакта **и** внешней конфигурации. Поэтому promotion проверяет:

1. digest находится в allowlisted registry и имеет доверенную provenance;
2. обязательные тесты/scans успешны, waiver не просрочен;
3. конфигурация и schema совместимы с новой и предыдущей версиями;
4. инициатор имеет право на среду; separation of duties соблюдён там, где требуется;
5. deployment window, health gates и rollback target определены.

Пример абстрактного release manifest:

```yaml
service: orders
version: 2.7.1
sourceCommit: 8a15d2c
artifact:
  uri: registry.example/orders
  digest: sha256:0123456789abcdef
evidence:
  provenance: registry.example/evidence/orders@sha256:aaaa
  sbom: registry.example/evidence/orders@sha256:bbbb
configurationRevision: 41de90a
```

## Audit trail

**Audit trail** должен позволить восстановить событие без ручного сопоставления разрозненных логов:

- кто/что инициировало и одобрило promotion;
- source commit, pipeline/workflow version и dependency inputs;
- artifact digest, signature/provenance/SBOM и policy verdict;
- environment, config revision, migration version, flag changes;
- время начала/окончания, rollout metrics, результат и rollback cause.

Логи должны быть append-only/защищены retention и доступом, время — синхронизировано, чувствительные данные — редактированы. Chat approval без устойчивого идентификатора не является достаточной записью.

## Типичные ошибки

- **Один tag перезаписывают:** включить registry immutability и deploy по digest.
- **Staging собирает своё:** promotion должен переносить тот же объект.
- **SBOM публикуют без связи:** привязать его к subject digest и provenance.
- **Конфиг не аудируется:** версионировать desired state и фиксировать revision.
- **Rollback artifact уже удалён:** retention должен покрывать rollback horizon.

## Практические упражнения

1. Спроектируйте release record для JAR и OCI image одного сервиса.
2. Опишите policy: кто может build, promote в staging и approve production.
3. Проверьте, сохраняет ли ваш registry digest при копировании между repositories.

## Вопросы для самопроверки

1. **Почему version tag недостаточен?** Его можно переназначить; digest адресует содержимое.
2. **Что меняется при promotion?** Допуск, metadata и desired environment state, но не байты артефакта.
3. **Заменяет ли SBOM scanner?** Нет: SBOM инвентаризирует компоненты, scanner соотносит их с findings/policy.
4. **Что нужно для rollback?** Предыдущий digest, совместимые данные/config, процедура и достаточный retention.

## Связанные темы

- [Построение CI pipeline](02-построение-ci-pipeline.html)
- [Релизы и deployment strategies](04-релизы-и-deployment-strategies.html)
- [AWS](../aws/README.html)

<script type="module" src="../assets/mermaid-init.js"></script>
