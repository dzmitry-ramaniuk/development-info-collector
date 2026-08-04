# Нагрузочное и performance-тестирование

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Модель нагрузки](#модель-нагрузки)
3. [Метрики](#метрики)
4. [Этапы и завершение](#этапы-и-завершение)
5. [Coordinated omission](#coordinated-omission)
6. [Воспроизводимый пример k6](#воспроизводимый-пример-k6)
7. [Разбор результата](#разбор-результата)
8. [Упражнения](#упражнения)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** Grafana k6 1.x; концепции применимы к HTTP-системам независимо от инструмента
- **Статус примеров:** `current`
- **Первичные источники:** [Grafana k6 documentation](https://grafana.com/docs/k6/latest/); [k6 executors](https://grafana.com/docs/k6/latest/using-k6/scenarios/executors/); [k6 thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/); [Google SRE: Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/)

## Модель нагрузки

**Workload model** описывает, кто, что, с какой частотой и как долго делает. Она включает:

- профили/сценарии и их доли (read/write/search, payload и cardinality);
- arrival pattern: средний трафик, пики, bursts, сезонность;
- concurrency, think time, кэш-хиты, распределение данных и доля ошибок;
- размер набора данных, topology, ресурсы, версии и сетевую задержку;
- duration: ramp-up, **warm-up**, steady state, spike/soak и ramp-down.

**Open model** задаёт arrivals независимо от скорости ответа; **closed model** держит число virtual users, поэтому при замедлении генерирует меньше запросов. Для внешнего потока чаще нужен open-model arrival-rate executor.

## Метрики

- **Latency percentiles**: p50 — типично, p95/p99 — хвост. Average скрывает редкие паузы. Отчёт должен указывать окно и объём выборки.
- **Throughput**: завершённые requests/transactions в секунду. Offered rate и achieved throughput не всегда равны: учитывайте dropped iterations и errors.
- **Saturation**: очередь за дефицитным ресурсом — CPU throttling, connection/thread pool, DB connections, queue lag, disk/network. CPU 100% — лишь один сигнал.
- **Errors**: доля ошибок по классам; бизнес-отказ и HTTP `5xx` не смешивают.

> Цифра latency без throughput, errors и saturation неинтерпретируема: «быстро» может означать быстрые отказы.

## Этапы и завершение

1. Зафиксировать hypothesis, SLO/thresholds, workload и бюджет остановки **до** запуска.
2. Smoke на малой нагрузке проверяет скрипт и данные.
3. **Warm-up** прогревает JIT, pools, caches и autoscaling. Его не скрывают: cold-start измеряют отдельно, а steady-state окно начинают после стабилизации.
4. Удерживать steady state достаточно для GC, autoscaling и циклов зависимостей.
5. Сравнить с **baseline**: та же среда, данные, workload, generator и окно. Хранить commit/image, config, raw summary и графики сервера.

**Критерии завершения:** заданная длительность/iteration count достигнуты; thresholds оценены; нет ошибки генератора; нагрузка достигнута; steady-state окно достаточно; артефакты сохранены. Аварийно остановить: опасная error rate, исчерпание диска/памяти, риск продукционным данным. Остановка не равна pass.

## Coordinated omission

**Coordinated omission** возникает, когда генератор ждёт медленный ответ и в это время не посылает запланированные запросы. Измеренный хвост выглядит лучше реального. Меры:

- arrival-rate/open-model executor для независимого потока;
- достаточно pre-allocated/max VUs и мониторинг `dropped_iterations`;
- сравнение intended arrival rate с achieved rate; dropped work не объявлять успехом.

## Воспроизводимый пример k6

Инструмент — **Grafana k6**. Файл `smoke.js` сам поднимает локальную HTTP-цель в Docker Compose; test URL для k6 внутри Compose — `http://target:5678`.

```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 10,
      maxVUs: 30,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<250', 'p(99)<500'],
    checks: ['rate>0.99'],
    dropped_iterations: ['count==0'],
  },
};

export default function () {
  const response = http.get(`${__ENV.BASE_URL}/status/200`, {
    tags: { operation: 'status' },
  });
  check(response, { 'status 200': (r) => r.status === 200 });
}
```

```yaml
# compose.yaml
services:
  target:
    image: hashicorp/http-echo:1.0
    command: ["-listen=:5678", "-status-code=200", "-text=ok"]
  k6:
    image: grafana/k6:1.2.0
    environment:
      BASE_URL: http://target:5678
    volumes:
      - ./smoke.js:/scripts/smoke.js:ro
    command: ["run", "/scripts/smoke.js"]
    depends_on: [target]
```

```bash
docker compose up --abort-on-container-exit --exit-code-from k6
docker compose down -v
```

Для реального baseline замените echo на зафиксированный image SUT, добавьте warm-up scenario без pass/fail метрик и длинное steady-state окно. Не запускайте нагрузку против production без явного согласования и ограничителей.

## Разбор результата

Pass требует одновременно: thresholds зелёные, 20 iterations/s достигнуты, dropped iterations равны нулю, генератор не saturated. Затем коррелируют latency с CPU, GC, pools, DB wait/locks и downstream. Одна корреляция не доказывает причину: изменяют один фактор и повторяют серию.

## Упражнения

1. Снимите workload из production-метрик, анонимизируйте данные и опишите допущения.
2. Замените target в примере на сервис с управляемой задержкой; наблюдайте VUs и dropped iterations.
3. Сравните два commit по пяти повторам и объясните, как отличаете регрессию от шума.

## Вопросы для самопроверки

1. **Почему p99 не следует читать без размера выборки?**
   На малой выборке хвостовой percentile нестабилен и описывает единицы наблюдений.
2. **Зачем измерять saturation?**
   Она показывает очередь и ограничивающий ресурс до полного отказа.
3. **Как closed model скрывает замедление?**
   Ждущие VUs не посылают новую работу, и offered load падает именно во время деградации.
