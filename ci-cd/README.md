# CI/CD

Раздел о том, как провести изменение от коммита до безопасного production-релиза: организовать Git workflow и code review, построить проверяемый pipeline, один раз собрать неизменяемый артефакт и управляемо продвигать его между средами.

## Содержание

1. [Git workflow и code review](01-git-и-code-review.md) — trunk-based development, GitFlow, небольшие pull request, обязательные проверки, Conventional Commits и SemVer.
2. [Построение CI pipeline](02-построение-ci-pipeline.md) — переносимый граф стадий и пример GitHub Actions для Java-проекта.
3. [Артефакты и promotion](03-артефакты-и-promotion.md) — immutable artifacts, provenance, registry, среды и audit trail.
4. [Релизы и стратегии deployment](04-релизы-и-deployment-strategies.md) — миграции БД, feature flags, rolling, blue-green, canary и rollback.

## Сквозной принцип

```text
малое изменение → review → воспроизводимая проверка → immutable artifact
                  → promotion того же digest → наблюдаемый rollout → аудит/rollback
```

CI отвечает на вопрос «можно ли доверять этому изменению и артефакту», а CD — «как доставить именно этот артефакт с контролируемым риском». Инструмент можно заменить, но контракт стадий, идентичность артефакта, правила допуска и доказательства выполнения должны сохраниться.

## Рекомендуемый маршрут

- **Разработчику:** файлы 01 → 02, затем чек-листы из 04.
- **Platform/DevOps-инженеру:** файлы 02 → 03 → 04.
- **Для проектирования процесса:** определите branch policy, quality gates, формат release evidence и только затем выбирайте CI/CD-платформу.

## Связанные разделы

- [Тестирование](../тестирование/README.md) — test pyramid, JUnit, Testcontainers и критерии качества.
- [Kubernetes](../kubernetes/README.md) — декларативный rollout и эксплуатация workloads.
- [AWS](../aws/README.md) — облачные IAM, registry, compute и deployment-сервисы.
- [System Design](../system%20design/README.md) — надёжность, наблюдаемость и архитектурные компромиссы.
- [Эволюция системы и миграции без простоя](../system%20design/12-эволюция-системы-и-миграции-без-простоя.md) — expand/contract, canary и изменение данных без downtime.

---

[← На главную](../README.md)
