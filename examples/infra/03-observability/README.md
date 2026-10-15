---
---

# PromQL: доли, единицы и границы alerts

Правила из [главы мониторинга](../../../kubernetes/05-мониторинг-логирование.html#alerting) вынесены в обычный Prometheus rule file без Kubernetes CRD. Baseline тестов — Prometheus/promtool 3.5.0; первоисточники сверены 2026-10-15: [rate](https://prometheus.io/docs/prometheus/latest/querying/functions/#rate), [rule unit tests](https://prometheus.io/docs/prometheus/latest/configuration/unit_testing_rules/).

```bash
cd examples/infra/03-observability
promtool check rules 01-rules.yaml
promtool test rules 02-rules.test.yaml
```

Восемь сценариев различают 1% и 10% ошибок при 100 и 10 000 запросов в минуту, подавляют сигнал при недостаточном/нулевом трафике, проверяют 95% memory и исключение нулевого лимита. В fixture метки `namespace/service` заданы явно; в вашем scrape pipeline они тоже должны присутствовать на обеих сторонах отношения.

Для Prometheus Operator поле `groups` переносится в `PrometheusRule.spec`; namespace/labels согласуются с `ruleSelector` выбранного Helm release. Эти offline-тесты не проверяют discovery, доставку уведомлений и совместимость chart/CRD. Отсутствие series или scraper требует отдельного alert, а не трактовки как 0% ошибок.
