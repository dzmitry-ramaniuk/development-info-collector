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

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** публичные API и документация AWS, проверенные 2026-08-04; версии управляемых сервисов уточняются в тексте
- **Статус примеров:** `current`
- **Первичные источники:** [AWS Documentation](https://docs.aws.amazon.com/); [AWS Architecture Center](https://aws.amazon.com/architecture/)

## Что решает Cognito

**Cognito** закрывает базовые задачи identity management: регистрация, логин, подтверждение пользователей, восстановление доступа, MFA.

## User Pool и токены

- User Pool хранит пользователей и политики аутентификации.
- После входа выдаются JWT-токены (ID/Access/Refresh).
- Токены используются в API Gateway/ALB/приложениях для авторизации.

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

[← К разделу AWS](README.md)
