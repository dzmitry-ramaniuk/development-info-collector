# Cognito: аутентификация, user pools и federation

## Содержание

1. [Что решает Cognito](#что-решает-cognito)
2. [User Pool и токены](#user-pool-и-токены)
3. [Identity Pool и доступ к AWS ресурсам](#identity-pool-и-доступ-к-aws-ресурсам)
4. [Federation и социальные логины](#federation-и-социальные-логины)
5. [Практический baseline для production](#практический-baseline-для-production)
6. [Вопросы для самопроверки](#вопросы-для-самопроверки)

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
