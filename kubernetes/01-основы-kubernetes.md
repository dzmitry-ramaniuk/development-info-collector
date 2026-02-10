# Основы Kubernetes

## Содержание

1. [Что такое Kubernetes](#что-такое-kubernetes)
2. [Зачем нужен Kubernetes](#зачем-нужен-kubernetes)
3. [История и эволюция](#история-и-эволюция)
4. [Основные концепции](#основные-концепции)
5. [Архитектура на высоком уровне](#архитектура-на-высоком-уровне)
6. [Установка и первые шаги](#установка-и-первые-шаги)
7. [Первое приложение в Kubernetes](#первое-приложение-в-kubernetes)
8. [Основные команды kubectl](#основные-команды-kubectl)

## Что такое Kubernetes

**Kubernetes** (часто сокращённо **K8s**) — это открытая платформа для автоматизации развёртывания, масштабирования и управления контейнеризированными приложениями. Название происходит от греческого слова «кормчий» или «рулевой».

### Ключевые характеристики:
- **Оркестрация контейнеров** — автоматическое управление жизненным циклом контейнеров
- **Декларативная конфигурация** — описываете желаемое состояние, а не последовательность команд
- **Самовосстановление** — автоматический перезапуск упавших контейнеров
- **Масштабирование** — горизонтальное и вертикальное масштабирование приложений
- **Service Discovery и Load Balancing** — автоматическое распределение нагрузки
- **Управление конфигурацией и секретами** — безопасное хранение чувствительных данных

## Зачем нужен Kubernetes

### Проблемы, которые решает Kubernetes:

1. **Управление множеством контейнеров**
   - Сложность запуска контейнеров на множестве хостов
   - Балансировка нагрузки между экземплярами
   - Автоматический перезапуск при сбоях

2. **Масштабирование**
   - Ручное масштабирование приложений трудоёмко
   - Автоматическое масштабирование на основе метрик
   - Эффективное использование ресурсов кластера

3. **Развёртывание обновлений**
   - Zero-downtime deployments
   - Откат к предыдущей версии при проблемах
   - Canary и Blue-Green развёртывания

4. **Service Discovery**
   - Автоматическое обнаружение сервисов
   - DNS для внутренней коммуникации
   - Абстракция от физических адресов

5. **Переносимость**
   - Работает в любом облаке (AWS, GCP, Azure)
   - Работает on-premise
   - Гибридные и мультиоблачные развёртывания

## История и эволюция

### Хронология развития:

**2003-2004**: Google разрабатывает **Borg** — внутреннюю систему управления контейнерами

**2013**: Docker делает контейнеры доступными широкой аудитории

**2014**: Google анонсирует Kubernetes как open-source проект
- Основан на опыте Borg и Omega
- Написан на Go
- Первый релиз v0.1

**2015**: 
- Версия 1.0
- Kubernetes передан Cloud Native Computing Foundation (CNCF)
- Поддержка от крупных компаний (Red Hat, Microsoft, IBM)

**2016-2017**: 
- Становится стандартом де-факто для оркестрации контейнеров
- Основные облачные провайдеры запускают managed Kubernetes сервисы

**2018-настоящее время**:
- Непрерывное развитие и улучшение
- Богатая экосистема инструментов
- Kubernetes everywhere — от edge до serverless

### Влияние на индустрию:

- **Cloud Native движение** — новый подход к разработке приложений
- **Микросервисная архитектура** — стала практичной благодаря K8s
- **DevOps и GitOps** — автоматизация через декларативные манифесты
- **Стандартизация** — единый API для различных облаков

## Основные концепции

### 1. Кластер (Cluster)
Набор машин (nodes), на которых запускаются контейнеризированные приложения, управляемые Kubernetes.

### 2. Node (Узел)
Рабочая машина в кластере Kubernetes. Может быть физическим сервером или виртуальной машиной.

**Типы узлов:**
- **Master Node (Control Plane)** — управляет кластером
- **Worker Node** — запускает приложения

### 3. Pod
Минимальная развёртываемая единица в Kubernetes. Один или несколько контейнеров, которые:
- Разделяют сетевой namespace (один IP-адрес)
- Разделяют storage volumes
- Запускаются на одном узле

### 4. Deployment
Декларативное описание желаемого состояния для Pods и ReplicaSets:
- Управление версиями приложения
- Rolling updates и rollbacks
- Масштабирование реплик

### 5. Service
Абстракция для доступа к группе Pods:
- Стабильный IP и DNS имя
- Load balancing между подами
- Service discovery

### 6. Namespace
Виртуальная изоляция ресурсов внутри кластера:
- Логическое разделение (dev, staging, prod)
- Квоты ресурсов
- Политики доступа

## Архитектура на высоком уровне

```
┌─────────────────────────────────────────────────┐
│               Kubernetes Cluster                │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │         Control Plane (Master)            │  │
│  │                                           │  │
│  │  ┌──────────┐  ┌──────────┐  ┌────────┐  │  │
│  │  │   API    │  │Scheduler │  │Control │  │  │
│  │  │  Server  │  │          │  │Manager │  │  │
│  │  └──────────┘  └──────────┘  └────────┘  │  │
│  │                                           │  │
│  │  ┌──────────┐                             │  │
│  │  │  etcd    │  (Key-Value хранилище)      │  │
│  │  └──────────┘                             │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  ┌───────────────┐  ┌───────────────┐          │
│  │ Worker Node 1 │  │ Worker Node 2 │  ...     │
│  │               │  │               │          │
│  │ ┌───────────┐ │  │ ┌───────────┐ │          │
│  │ │  Kubelet  │ │  │ │  Kubelet  │ │          │
│  │ └───────────┘ │  │ └───────────┘ │          │
│  │               │  │               │          │
│  │ ┌───────────┐ │  │ ┌───────────┐ │          │
│  │ │   Pods    │ │  │ │   Pods    │ │          │
│  │ └───────────┘ │  │ └───────────┘ │          │
│  └───────────────┘  └───────────────┘          │
└─────────────────────────────────────────────────┘
```

## Установка и первые шаги

### Варианты локальной установки:

#### 1. Minikube
Локальный однонодовый кластер для разработки и обучения.

```bash
# Установка Minikube (macOS)
brew install minikube

# Запуск кластера
minikube start

# Проверка статуса
minikube status

# Остановка
minikube stop
```

#### 2. Kind (Kubernetes IN Docker)
Запускает Kubernetes кластер в Docker контейнерах.

```bash
# Установка Kind
brew install kind

# Создание кластера
kind create cluster

# Удаление кластера
kind delete cluster
```

#### 3. Docker Desktop
Встроенный Kubernetes в Docker Desktop (Windows/macOS).

```
Settings → Kubernetes → Enable Kubernetes
```

#### 4. K3s/K3d
Лёгкая версия Kubernetes для edge и IoT.

### Установка kubectl

**kubectl** — командная строка для работы с Kubernetes.

```bash
# macOS
brew install kubectl

# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# Проверка
kubectl version --client
```

### Проверка подключения к кластеру

```bash
# Информация о кластере
kubectl cluster-info

# Список узлов
kubectl get nodes

# Список всех ресурсов
kubectl get all
```

## Первое приложение в Kubernetes

### Создание простого Deployment

```yaml
# nginx-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.21
        ports:
        - containerPort: 80
```

```bash
# Применить манифест
kubectl apply -f nginx-deployment.yaml

# Проверить статус
kubectl get deployments
kubectl get pods

# Посмотреть подробности
kubectl describe deployment nginx-deployment
```

### Создание Service для доступа

```yaml
# nginx-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  type: LoadBalancer
  selector:
    app: nginx
  ports:
  - port: 80
    targetPort: 80
```

```bash
# Применить Service
kubectl apply -f nginx-service.yaml

# Проверить Service
kubectl get services

# Для Minikube — получить URL
minikube service nginx-service --url
```

### Масштабирование приложения

```bash
# Увеличить количество реплик
kubectl scale deployment nginx-deployment --replicas=5

# Проверить
kubectl get pods
```

### Обновление приложения

```bash
# Обновить образ
kubectl set image deployment/nginx-deployment nginx=nginx:1.22

# Проверить статус обновления
kubectl rollout status deployment/nginx-deployment

# Посмотреть историю
kubectl rollout history deployment/nginx-deployment

# Откатить изменения
kubectl rollout undo deployment/nginx-deployment
```

## Основные команды kubectl

### Работа с ресурсами

```bash
# Получить список ресурсов
kubectl get <resource>              # pods, services, deployments, etc.
kubectl get pods -o wide            # Расширенная информация
kubectl get pods -n <namespace>     # В конкретном namespace

# Подробная информация
kubectl describe <resource> <name>
kubectl describe pod nginx-pod

# Создать ресурс
kubectl apply -f <file.yaml>
kubectl create deployment nginx --image=nginx

# Удалить ресурс
kubectl delete <resource> <name>
kubectl delete -f <file.yaml>
```

### Отладка и логи

```bash
# Логи пода
kubectl logs <pod-name>
kubectl logs -f <pod-name>          # Следить за логами
kubectl logs <pod-name> -c <container>  # Конкретный контейнер

# Выполнить команду в контейнере
kubectl exec -it <pod-name> -- /bin/bash
kubectl exec <pod-name> -- ls /app

# Port forwarding
kubectl port-forward <pod-name> 8080:80
```

### Конфигурация и контекст

```bash
# Текущий контекст
kubectl config current-context

# Список контекстов
kubectl config get-contexts

# Переключить контекст
kubectl config use-context <context-name>

# Установить namespace по умолчанию
kubectl config set-context --current --namespace=<namespace>
```

### Полезные алиасы

```bash
# Добавьте в ~/.bashrc или ~/.zshrc
alias k=kubectl
alias kgp='kubectl get pods'
alias kgs='kubectl get services'
alias kgd='kubectl get deployments'
alias kdp='kubectl describe pod'
alias kl='kubectl logs'
alias kex='kubectl exec -it'
```

## Практические упражнения

### Упражнение 1: Развернуть приложение
1. Создайте Deployment с 3 репликами nginx
2. Создайте Service для доступа к приложению
3. Проверьте, что все поды запущены
4. Получите доступ к приложению через браузер

### Упражнение 2: Масштабирование
1. Увеличьте количество реплик до 5
2. Посмотрите, как Kubernetes распределяет поды по узлам
3. Уменьшите до 2 реплик
4. Наблюдайте за процессом удаления подов

### Упражнение 3: Обновление приложения
1. Обновите версию nginx в Deployment
2. Наблюдайте за процессом rolling update
3. Проверьте историю обновлений
4. Откатите к предыдущей версии

## Вопросы для самопроверки

1. **Что такое Kubernetes и какие проблемы он решает?**
   - Платформа оркестрации контейнеров для автоматизации развёртывания и управления
   - Решает проблемы масштабирования, отказоустойчивости, service discovery

2. **В чём разница между Pod и Deployment?**
   - Pod — минимальная единица, один или несколько контейнеров
   - Deployment — декларативное управление Pods с версионированием и масштабированием

3. **Зачем нужен Service в Kubernetes?**
   - Стабильный endpoint для доступа к группе Pods
   - Load balancing и service discovery

4. **Что такое декларативный подход?**
   - Описываете желаемое состояние системы, а не команды для его достижения
   - Kubernetes сам приводит систему к желаемому состоянию

5. **Какие есть варианты локальной установки Kubernetes?**
   - Minikube, Kind, Docker Desktop, K3s/K3d

---

Эти основы помогут начать работу с Kubernetes и понять его ключевые концепции. В следующих разделах мы подробно рассмотрим архитектуру компонентов, развёртывание приложений и операционные аспекты.
