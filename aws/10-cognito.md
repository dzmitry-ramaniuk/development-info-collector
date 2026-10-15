---
---

# Cognito: аутентификация, user pools и federation

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Что решает Cognito](#что-решает-cognito)
3. [User Pool и токены](#user-pool-и-токены)
4. [Identity Pool и доступ к AWS ресурсам](#identity-pool-и-доступ-к-aws-ресурсам)
5. [Federation и социальные логины](#federation-и-социальные-логины)
6. [Практический baseline для production](#практический-baseline-для-production)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

> **Дата ревизии:** 4 августа 2026 года. Числовые значения проверены на эту дату; доступность функций, цены и quotas зависят от региона, типа аккаунта и одобренных AWS повышений. Перед production-развёртыванием сверяйтесь с Service Quotas и AWS Console.

**Официальные источники:** [service documentation](https://docs.aws.amazon.com/cognito/latest/developerguide/what-is-amazon-cognito.html) · [quotas](https://docs.aws.amazon.com/cognito/latest/developerguide/quotas.html) · [pricing](https://aws.amazon.com/cognito/pricing/)
## Актуальность материала

- **Дата проверки исправлений:** 2026-10-15; назначение и проверка токенов. Остальная редакция — 2026-08-04, её повторная сверка не заявляется.
- **Целевая версия или диапазон:** публичные API и документация AWS, проверенные 2026-08-04; версии управляемых сервисов уточняются в тексте
- **Статус примеров:** `current`
- **Первичные источники исправлений:** [Verifying JWTs](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html); [Refresh tokens](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-the-refresh-token.html).

## Что решает Cognito

**Cognito** закрывает базовые задачи identity management: регистрация, логин, подтверждение пользователей, восстановление доступа, MFA.

## User Pool и токены

- User Pool хранит пользователей и политики аутентификации.
- ID token и access token — подписанные JWT с разным назначением. Refresh token — непрозрачный credential для обновления сессии: приложение не должно декодировать его как JWT или передавать своему API как access token.

| Токен | Назначение | Проверка/использование |
|---|---|---|
| ID token | Сведения о вошедшем пользователе для клиента | `token_use=id`, `aud` клиента; не использовать вместо access token в показанном API |
| Access token | Разрешения на вызов API | Подпись, `iss`, `exp`, `token_use=access`, `client_id` и требуемые scopes |
| Refresh token | Получение новых ID/access tokens | Передавать только token endpoint/поддерживаемому Cognito refresh API; хранить как секрет |

Для API задайте доверенные user pool/issuer и app client в конфигурации сервера. JWT-библиотека должна проверить разрешённый алгоритм, подпись по JWKS этого pool (с кэшем и обновлением ключей), срок действия, `token_use=access`, `client_id` и scope, например `orders/read`. Непроверенный payload не является доказательством доступа. Для resource-bound access token дополнительно проверяют предназначенный API `aud`; ID token проверяет `aud` клиента по другим правилам.

Минимальный контракт интеграционного теста для защищённого `GET /orders`:

| Вход | Ожидаемый результат |
|---|---|
| Подписанный access token нужного pool/client со scope `orders/read` | 200 |
| ID token, refresh token, истёкший JWT, чужой issuer/client или неверная подпись | 401 |
| Валидный access token без `orders/read` | 403 |

Не подменяйте JWT verification вызовом `GetUser`. Настройки API Gateway/ALB отличаются от собственного verifier: отдельно проверяйте, какие claims и scopes проверяет конкретная интеграция. См. [проверку Cognito JWT](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html) и [refresh API](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-the-refresh-token.html).

## Identity Pool и доступ к AWS ресурсам

- Identity Pool выдаёт временные AWS credentials.
- Позволяет фронтенду безопасно обращаться к AWS-ресурсам (например, S3) по role-based правилам.

## Federation и социальные логины

- Подключение внешних IdP через OIDC/SAML.
- Поддержка social login (Google, Apple и т.д.).
- Удобно для B2C-сценариев и SSO-интеграций.

## Практический baseline для production

1. MFA для чувствительных сценариев.
2. Password policy и защита от brute-force.
3. Короткие access token TTL + безопасное хранение refresh token.
4. Audit logging и мониторинг подозрительной активности.
5. Чёткое разделение ролей и прав на уровне API.

## Вопросы для самопроверки

1. Когда нужен только User Pool, а когда ещё и Identity Pool?
2. Как безопасно хранить и обновлять токены в клиенте?
3. Какие риски возникают при federation и как их снизить?

---

[← К разделу AWS](README.html)
