# Kubernetes

## Содержание

1. [📚 Содержание](#-содержание)
2. [🧭 Рекомендуемые маршруты по разделу](#-рекомендуемые-маршруты-по-разделу)
3. [🎯 Как использовать](#-как-использовать)
   - [Для начинающих](#для-начинающих)
   - [Для подготовки к собеседованиям](#для-подготовки-к-собеседованиям)
   - [Для DevOps инженеров](#для-devops-инженеров)
4. [💡 Рекомендации](#-рекомендации)
5. [⚠️ Важные замечания](#-важные-замечания)
6. [🔗 Связанные темы](#-связанные-темы)


> **Проверенный baseline:** Kubernetes **1.34** (стабильный minor-релиз). Все встроенные `apiVersion`, манифесты и команды на этой странице сверены с документацией v1.34. Для кластера с `kube-apiserver` 1.34: `kubectl` поддерживается в диапазоне **1.33–1.35**; `kubelet` не может быть новее API server и поддерживается в диапазоне **1.31–1.34**; `kube-controller-manager`, `kube-scheduler` и `cloud-controller-manager` не должны быть новее `kube-apiserver` и могут отставать не более чем на один minor. В HA control plane версии `kube-apiserver` могут различаться не более чем на один minor. Подробнее: [Version Skew Policy](https://v1-34.docs.kubernetes.io/releases/version-skew-policy/).


Baseline зафиксирован намеренно: перед переходом на другой minor повторно проверьте [deprecated/removed API](https://v1-34.docs.kubernetes.io/reference/using-api/deprecation-guide/), feature gates и документацию поставщика managed-кластера. Диапазон skew — это поддерживаемая совместимость, а не рекомендация откладывать обновления узлов.

Полное руководство по Kubernetes — системе оркестрации контейнеров для автоматизации развёртывания, масштабирования и управления контейнеризированными приложениями.

## 📚 Содержание

1. [Основы Kubernetes](01-основы-kubernetes.md)
   - Что такое Kubernetes и зачем он нужен
   - История и эволюция
   - Основные концепции и архитектура
   - Установка и первые шаги

2. [Архитектура и компоненты](02-архитектура-компоненты.md)
   - Архитектура кластера
   - Control Plane компоненты
   - Node компоненты
   - Сетевая модель
   - Хранение данных

3. [Развёртывание и сервисы](03-деплоймент-сервисы.md)
   - Pods и контейнеры
   - Deployments и ReplicaSets
   - Services и типы сервисов
   - Ingress и маршрутизация
   - StatefulSets и DaemonSets

4. [Конфигурация и секреты](04-конфигурация-секреты.md)
   - ConfigMaps
   - Secrets
   - Environment variables
   - Volumes и Persistent Volumes
   - Storage Classes

5. [Мониторинг и логирование](05-мониторинг-логирование.md)
   - Мониторинг кластера
   - Prometheus и Grafana
   - Логирование приложений
   - EFK/ELK стеки
   - Трассировка и observability

6. [Безопасность и политики](06-безопасность-и-политики.md)
   - ServiceAccount и least-privilege RBAC
   - NetworkPolicy и Pod Security Standards
   - Шифрование Secrets и policy engines

7. [Scheduling, ресурсы и autoscaling](07-scheduling-и-autoscaling.md)
   - Requests, limits, QoS и probes
   - PDB, affinity, topology spread, taints/tolerations
   - HPA, VPA и Cluster Autoscaler

8. [Доставка приложений](08-доставка-приложений.md)
   - Helm, Kustomize, GitOps и CI/CD
   - Rolling, canary и blue-green rollout
   - Проверяемый безопасный rollback

9. [Troubleshooting и disaster recovery](09-troubleshooting-и-disaster-recovery.md)
   - Runbooks для workloads, DNS/сети и storage
   - Диагностика деградации control plane
   - RTO/RPO, backup и регулярная проверка restore

## 🧭 Рекомендуемые маршруты по разделу

- **Если начинаете с нуля**: идите строго по порядку файлов — от базовых концепций и архитектуры к deployment-паттернам, конфигурации и observability
- **Если уже деплоили приложения, но хотите лучше понимать production**: сосредоточьтесь на архитектуре, ресурсах, безопасности, доставке, мониторинге и [операционных runbooks](09-troubleshooting-и-disaster-recovery.md)
- **Если готовитесь к DevOps/SRE-интервью**: разберите scheduling/autoscaling, [границы security-механизмов](06-безопасность-и-политики.md) и отрепетируйте troubleshooting по симптомам

## 🎯 Как использовать

### Для начинающих
1. Начните с основ — изучите концепции контейнеризации и зачем нужна оркестрация
2. Установите локальный кластер (Minikube или Kind) для практики
3. Последовательно изучайте материалы, выполняя примеры
4. Практикуйтесь с kubectl и изучайте yaml-манифесты

### Для подготовки к собеседованиям
1. Сосредоточьтесь на архитектуре и компонентах
2. Изучите типы ресурсов и их назначение
3. Разберитесь с сетевой моделью и хранением данных
4. Практикуйтесь в написании манифестов
5. Изучите best practices для production

### Для DevOps инженеров
1. Обратите внимание на эксплуатацию и автоматизацию
2. Изучите мониторинг и логирование
3. Практикуйтесь с CI/CD интеграцией
4. Изучите Helm и управление релизами
5. Освойте troubleshooting и отладку

## 💡 Рекомендации

- **Практикуйтесь локально**: Используйте Minikube, Kind или Docker Desktop
- **Изучайте kubectl**: Основной инструмент для работы с Kubernetes
- **Читайте документацию**: Официальная документация Kubernetes очень качественная
- **Пишите yaml**: Декларативный подход — основа инфраструктуры как кода
- **Тестируйте**: Всегда проверяйте изменения в dev-окружении перед production

## ⚠️ Важные замечания

> **Декларативный подход**: Kubernetes работает с желаемым состоянием (desired state), а не императивными командами

> **Иммутабельность**: Контейнеры должны быть неизменяемыми; изменения вносятся через новые версии образов

> **Безопасность**: RBAC, Network Policies, Pod Security Standards критичны для production

> **Ресурсы**: Всегда указывайте requests и limits для CPU и памяти

## 🔗 Связанные темы

Для полного понимания Kubernetes рекомендуется знание:
- **Docker**: Контейнеризация приложений
- **Linux**: Основы работы с ОС, сетью, файловой системой
- **Сети**: TCP/IP, DNS, балансировка нагрузки
- **Инфраструктура**: Облачные провайдеры (AWS, GCP, Azure)
- [System Design](../system%20design/README.md) — общий контекст масштабирования, observability и проектирования production-ready систем
- [Масштабирование, надёжность и отказоустойчивость](../system%20design/06-масштабирование-надежность-и-отказоустойчивость.md) — связь между архитектурными решениями и оркестрацией
- [Наблюдаемость, безопасность и эксплуатация](../system%20design/08-наблюдаемость-безопасность-и-эксплуатация.md) — как использовать метрики, алерты и security practices до и после деплоя
- [AWS](../aws/README.md) — облачная инфраструктура, IAM, сеть, storage и managed Kubernetes-контекст
- [CI/CD и GitOps](08-доставка-приложений.md#supply-chain-и-cicd) — pipeline, promotion, progressive delivery и rollback
- [Troubleshooting и disaster recovery](09-troubleshooting-и-disaster-recovery.md) — связь архитектурных RTO/RPO с эксплуатационными runbooks
- [CI/CD: релизы и deployment strategies](../ci-cd/04-релизы-и-deployment-strategies.md) — rolling, blue-green, canary, health gates и rollback

---

[← Назад к главной странице](../README.md)
