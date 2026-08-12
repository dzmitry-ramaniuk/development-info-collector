---
---

# Security testing

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Модель и границы](#модель-и-границы)
3. [Dependency scanning](#dependency-scanning)
4. [SAST и DAST](#sast-и-dast)
5. [Контроль доступа](#контроль-доступа)
6. [OWASP API Security Top 10](#owasp-api-security-top-10)
7. [Секреты](#секреты)
8. [Стратегия CI/CD](#стратегия-cicd)
9. [Упражнения](#упражнения)
10. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** OWASP API Security Top 10 — 2023; OWASP ASVS 5.0; CycloneDX 1.6; NIST SSDF 1.1
- **Статус примеров:** `current`
- **Первичные источники:** [OWASP API Security Top 10 — 2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/); [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/); [NIST SSDF](https://csrc.nist.gov/pubs/sp/800/218/final); [CycloneDX specification](https://cyclonedx.org/specification/overview/)

## Модель и границы

Начните с threat model: assets, actors, trust boundaries, entry points, abuse cases и controls. Security testing снижает неопределённость, но «scanner found nothing» не доказывает безопасность. Важны и secure design/review, least privilege, hardening, monitoring, response и ручной pentest по риску.

> Тестируйте только системы, на которые есть явное разрешение; фиксируйте scope, rate limits, test data, окно и stop conditions.

## Dependency scanning

**Software composition analysis (SCA)** инвентаризирует direct/transitive dependencies, containers и runtime packages, сопоставляя их с advisories. Практика:

1. Генерировать SBOM (CycloneDX/SPDX) из финального артефакта, хранить с digest/provenance.
2. Сканировать PR, default branch, registry и периодически пересканировать без rebuild.
3. Триаж учитывает reachability, exploitability, exposure и asset criticality, а не только CVSS.
4. Политика задаёт SLA по severity/exploitability; exception имеет owner, justification, compensating control и expiry.
5. Отдельно проверять лицензии, typosquatting, abandoned package и integrity locks/signatures.

## SAST и DAST

| Метод | Видит | Не видит | Место |
|---|---|---|---|
| **SAST** | source/bytecode, data flow, dangerous API, injection paths | runtime config, proxy, deployed auth | IDE/PR/full scan |
| **DAST** | наблюдаемое поведение запущенного приложения | недостижимый код и полную причинную цепочку | изолированный deploy/staging |
| SCA | известные компоненты/CVE/лицензии | собственную бизнес-логику | build + registry + rescan |
| Secret scanning | похожие на credentials строки и история | все runtime leaks/неизвестные форматы | pre-commit, push, history |

SAST на PR запускайте для изменённого кода, полный анализ — периодически. Baseline старого долга не должен скрывать новые findings. DAST получает seed из OpenAPI, отдельные роли и безопасные тестовые данные; active scan не направляют на production без особого плана. Findings дедуплицируют, подтверждают и связывают с CWE/control; «false positive» документируют, а не отключают правило глобально.

## Контроль доступа

Матрица **subject × action × resource × context** должна включать allow и deny:

| Субъект | Свой объект | Чужой объект того же tenant | Другой tenant | Admin action |
|---|---:|---:|---:|---:|
| User | allow по политике | deny | deny | deny |
| Tenant admin | allow | allow по политике tenant | deny | deny/ограниченно |
| Platform admin | по регламенту | по регламенту | по регламенту | allow с аудитом |

Для каждого endpoint меняйте ID в path/query/body, HTTP method, nested ID и batch elements. Проверяйте не только response, но и отсутствие записи/события/cache side effect, а также audit event. Deny-by-default и централизованная policy уменьшают пропуски; UI-скрытие кнопки не является контролем.

## OWASP API Security Top 10

Версия 2023 задаёт risk checklist, но не исчерпывающий test plan:

1. **API1 BOLA:** подмена object ID; проверка ownership на каждом объекте.
2. **API2 Broken Authentication:** token lifecycle, credential stuffing defenses, recovery/MFA.
3. **API3 Broken Object Property Level Authorization:** чтение/запись запрещённых полей, mass assignment.
4. **API4 Unrestricted Resource Consumption:** размер, rate, pagination, timeout и cost limits.
5. **API5 Broken Function Level Authorization:** вызов admin/function другой ролью или методом.
6. **API6 Unrestricted Access to Sensitive Business Flows:** автоматизация покупки/регистрации/резерва, anti-abuse.
7. **API7 SSRF:** URL/redirect/DNS inputs не должны достигать internal/metadata networks.
8. **API8 Security Misconfiguration:** CORS, TLS, methods, headers, debug endpoints, verbose errors.
9. **API9 Improper Inventory Management:** неизвестные версии/hosts, shadow/deprecated API, актуальная спецификация.
10. **API10 Unsafe Consumption of APIs:** недоверенные downstream data, schema/limits/timeouts и безопасные redirects.

## Секреты

Проверяйте весь жизненный цикл:

- секрет поступает из secret manager/короткоживущей identity, не из репозитория, образа или frontend bundle;
- secret scanning охватывает staged changes, commit history, build logs и artifacts;
- логи/trace/error response маскируют `Authorization`, cookies, keys, connection strings и PII; тест отправляет canary secret и ищет его во всех выходах;
- CI не печатает секрет через shell tracing, process arguments или небезопасный artifact; fork PR не получает production credentials;
- least privilege, environment isolation, rotation/revocation проверяются rehearsal; после утечки сначала отзывают ключ, затем удаляют из истории;
- временные файлы, heap dump, support bundle и backup также входят в threat model.

```bash
# Локальная учебная проверка tracked-файлов на фиктивный canary, не настоящий ключ
canary='TEST_SECRET_DO_NOT_USE_7f3a'
if git grep -n --fixed-strings "$canary"; then
  echo 'canary попал в tracked-файл' >&2
  exit 1
fi
```

> Удаление строки из последнего commit не обезвреживает опубликованный секрет: credential нужно немедленно отозвать/ротировать.

## Стратегия CI/CD

- **Каждый commit:** compiler/unit, SAST diff, SCA policy, secret scan, IaC/container config.
- **PR/deploy preview:** contract/API access-control tests, passive/targeted DAST.
- **Nightly/release:** полный SAST/DAST, container/SBOM rescan, abuse/fuzz по лимитам.
- **Периодически и после значимых изменений:** threat-model review, ручной pentest, restore/rotation/incident exercise.

Gate должен быть risk-based и воспроизводимым. Сохраняйте tool/rule/database version, artifact digest и scope. Экстренный bypass требует одобрения, компенсирующего контроля и срока; отчёт сканера не публикуют с рабочими secrets или exploit details.

## Упражнения

1. Постройте матрицу доступа для API документов и реализуйте deny-тесты другого tenant и bulk endpoint.
2. Создайте SBOM учебного контейнера, найдите transitive package и оформите triage с expiry.
3. Передайте canary token через request и убедитесь, что он отсутствует в response, logs, traces и CI artifact.
4. По OpenAPI выберите по одному сценарию для каждого OWASP API риска и укажите безопасные stop conditions.

## Вопросы для самопроверки

1. **Почему CVSS недостаточно для приоритета?**
   Он не учитывает полностью достижимость, exposure, бизнес-критичность и компенсирующие controls конкретной системы.
2. **Чем SAST дополняет DAST?**
   SAST видит пути кода/data flow, DAST — реально наблюдаемое поведение deployed-конфигурации.
3. **Что обязательно проверить после ответа `403`?**
   Отсутствие side effect и утечки, корректный audit event; сам status не доказывает запрет.
4. **Что делать при попадании ключа в Git?**
   Немедленно отозвать/ротировать, оценить использование, затем очищать историю и исправлять канал доставки.
