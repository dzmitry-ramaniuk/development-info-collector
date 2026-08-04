# Spring Security

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** Java 17–25, Spring Security 6.5.x, Spring Boot 3.5.x
- **Статус примеров:** `current`
- **Первичные источники:** [Spring Security Reference](https://docs.spring.io/spring-security/reference/6.5/); [Spring Boot Reference](https://docs.spring.io/spring-boot/3.5/reference/); [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://www.rfc-editor.org/rfc/rfc9700); [JSON Web Token Best Current Practices (RFC 8725)](https://www.rfc-editor.org/rfc/rfc8725)

> Материал описывает servlet-приложения. Для WebFlux используются `SecurityWebFilterChain` и реактивный `SecurityContext`; смешивать servlet- и reactive API нельзя.

## Содержание

1. [Модель безопасности Spring Security](#модель-безопасности-spring-security)
   - [SecurityFilterChain и цепочка фильтров](#securityfilterchain-и-цепочка-фильтров)
   - [Authentication и SecurityContext](#authentication-и-securitycontext)
2. [Аутентификация и хранение паролей](#аутентификация-и-хранение-паролей)
   - [Session-based authentication](#session-based-authentication)
   - [Token-based authentication](#token-based-authentication)
   - [Безопасное хранение паролей](#безопасное-хранение-паролей)
3. [Рабочая конфигурация Spring Boot](#рабочая-конфигурация-spring-boot)
   - [Зависимости](#зависимости)
   - [Session-based приложение](#session-based-приложение)
   - [OAuth 2.0 Resource Server с JWT](#oauth-20-resource-server-с-jwt)
4. [OAuth 2.0, OIDC и проверка JWT](#oauth-20-oidc-и-проверка-jwt)
   - [Разделение ролей протоколов](#разделение-ролей-протоколов)
   - [Правила валидации JWT](#правила-валидации-jwt)
   - [Refresh-токены и ротация ключей](#refresh-токены-и-ротация-ключей)
5. [Авторизация: RBAC и method security](#авторизация-rbac-и-method-security)
6. [Защита HTTP-границы](#защита-http-границы)
   - [CSRF](#csrf)
   - [CORS](#cors)
   - [Secure headers](#secure-headers)
   - [HTTP 401 и 403](#http-401-и-403)
7. [Logout, отзыв и завершение сессий](#logout-отзыв-и-завершение-сессий)
8. [Тестирование с spring-security-test](#тестирование-с-spring-security-test)
9. [Типичные ошибки и практический чек-лист](#типичные-ошибки-и-практический-чек-лист)
10. [Практическое упражнение](#практическое-упражнение)
11. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Модель безопасности Spring Security

Spring Security решает две разные задачи:

- **аутентификация** отвечает на вопрос «кто выполняет запрос?»;
- **авторизация** решает, разрешено ли этому субъекту конкретное действие.

Безопасность должна проверяться на сервере для каждого защищённого запроса. Скрытая кнопка в UI, проверка роли в JavaScript или доверие полям `userId` и `role` из тела запроса не являются контролем доступа.

### SecurityFilterChain и цепочка фильтров

Современная конфигурация публикует один или несколько бинов `SecurityFilterChain`. Унаследованный адаптер `WebSecurityConfigurerAdapter` больше не используется. Servlet-контейнер передаёт запрос в `DelegatingFilterProxy`, затем `FilterChainProxy` выбирает **первую** цепочку, чей `securityMatcher` совпал с запросом.

Упрощённый поток запроса:

1. фильтры загружают ранее сохранённый `SecurityContext`;
2. аутентифицирующий фильтр извлекает credentials — например, логин/пароль или Bearer token;
3. `AuthenticationManager` делегирует подходящему `AuthenticationProvider`;
4. успешный результат помещается в `SecurityContextHolder` и, если требуется, сохраняется через `SecurityContextRepository`;
5. `AuthorizationFilter` проверяет правило доступа;
6. `ExceptionTranslationFilter` преобразует отсутствие аутентификации в запуск `AuthenticationEntryPoint`, а отказ авторизации — в `AccessDeniedHandler`;
7. контекст очищается по завершении запроса.

Точный состав и порядок фильтров зависят от включённых механизмов. Нельзя произвольно добавлять JWT-фильтр «перед каким-нибудь фильтром»: сначала следует проверить, не реализует ли требуемый протокол штатный DSL (`oauth2ResourceServer`, `oauth2Login`, `formLogin`). Для диагностики полезно временно включить `logging.level.org.springframework.security=TRACE`, не оставляя TRACE в production из-за объёма и чувствительности логов.

Если объявлено несколько цепочек, задавайте `@Order`, узкий `securityMatcher` и резервную цепочку:

```java
@Bean
@Order(1)
SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/actuator/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().hasRole("OPS"))
        .httpBasic(Customizer.withDefaults());
    return http.build();
}
```

> `securityMatcher` выбирает всю цепочку, а `requestMatchers` внутри `authorizeHttpRequests` выбирают правило авторизации. Если ни одна цепочка не совпала, Spring Security запрос не защищает.

### Authentication и SecurityContext

`Authentication` одновременно представляет запрос на аутентификацию и подтверждённую identity:

- `principal` — пользователь или идентификатор субъекта;
- `credentials` — секрет; после успешной проверки обычно стирается;
- `authorities` — разрешения (`GrantedAuthority`);
- `authenticated` — признак результата, который должен выставлять доверенный provider, а не контроллер.

`SecurityContext` содержит текущий `Authentication`, а `SecurityContextHolder` связывает контекст с текущим выполнением. В servlet-приложении по умолчанию применяется `ThreadLocal`; поэтому контекст нельзя бездумно переносить в вручную созданные потоки. Для executor используйте поддерживаемые security-aware wrappers либо явно передавайте нужную identity. После выполнения контекст должен быть очищен.

Контроллер может получить principal без обращения к глобальному holder:

```java
@GetMapping("/me")
Map<String, Object> me(Authentication authentication) {
    return Map.of(
        "name", authentication.getName(),
        "authorities", authentication.getAuthorities()
    );
}
```

## Аутентификация и хранение паролей

### Session-based authentication

При session-based подходе сервер после логина сохраняет `SecurityContext` в HTTP session, а браузер передаёт только cookie с идентификатором сессии. Это удобный выбор для server-rendered приложений и BFF:

- сервер может немедленно завершить сессию;
- cookie следует помечать `Secure`, `HttpOnly` и осмысленным `SameSite`;
- нужны защита от фиксации сессии, CSRF-защита и управление временем жизни;
- кластеру требуется общее хранилище сессий либо иной согласованный механизм маршрутизации.

Spring Security меняет идентификатор сессии после логина для защиты от session fixation. Не помещайте пароль, access token или лишние персональные данные в session.

### Token-based authentication

В token-based API клиент посылает `Authorization: Bearer <access-token>`. Resource Server валидирует токен на каждом запросе и обычно не создаёт HTTP session (`SessionCreationPolicy.STATELESS`).

JWT не является синонимом «безопасный» и по умолчанию не шифрует payload: Base64URL-кодированные claims доступны предъявителю. Альтернатива JWT — opaque token, который Resource Server проверяет интроспекцией у Authorization Server. JWT уменьшает сетевую зависимость при проверке, но затрудняет немедленный отзыв; introspection даёт централизованный статус, но добавляет сетевой вызов и требования к устойчивости.

### Безопасное хранение паролей

Пароль хранят только как результат адаптивной односторонней password hashing function с уникальной солью. В Spring Security предпочтителен `DelegatingPasswordEncoder`: префикс вида `{bcrypt}` позволяет мигрировать алгоритм и параметры без одновременного сброса всех паролей.

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

Для новых записей можно явно выбрать Argon2id, bcrypt, PBKDF2 или scrypt с параметрами, измеренными на своей инфраструктуре. Проверка должна занимать заметное, но приемлемое время; параметры периодически повышают. При успешном логине старый hash можно обновить через `UserDetailsPasswordService`.

Нельзя:

- хранить plaintext, обратимо зашифрованные пароли или быстрые hash-функции SHA-256/MD5 без password KDF;
- писать пароль, токен или полный `Authentication` в лог;
- сравнивать hashes самостоятельно или использовать одну общую «соль»;
- кодировать уже закодированный пароль повторно при каждом чтении пользователя.

## Рабочая конфигурация Spring Boot

Ниже приведены две **альтернативные** конфигурации: stateful web-приложение и stateless Resource Server. В реальном проекте их удобно разделить профилями или отдельными приложениями. Если они нужны вместе, используйте упорядоченные цепочки с непересекающимися `securityMatcher`.

### Зависимости

Минимальный Maven-фрагмент для Spring MVC, Resource Server и тестов (версии управляются Spring Boot parent/BOM):

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Session-based приложение

```java
package example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
class WebSecurityConfig {

    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"));

        // CSRF, session-fixation protection и стандартные security headers
        // намеренно оставлены включёнными.
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

Пользователей production-системы загружает собственный `UserDetailsService`/`AuthenticationProvider` из БД или внешний identity provider. Не оставляйте сгенерированный Boot-пароль и in-memory users как production-модель.

### OAuth 2.0 Resource Server с JWT

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://id.example.com/realms/orders
          audiences: https://api.example.com/orders
```

```java
package example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
class ApiSecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter)
            throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/**").hasAuthority("SCOPE_orders.read")
                .anyRequest().authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
```

Отключение CSRF здесь обосновано только тем, что `/api/**` принимает Bearer token из `Authorization` и не использует автоматически прикрепляемые браузером credentials. Если браузер аутентифицируется cookie, CSRF необходимо оставить включённым, даже если значение cookie содержит JWT.

`issuer-uri` включает discovery ключей и проверку `iss`; свойство `audiences` требует ожидаемую аудиторию. Для нестандартных claims или более ранних версий конфигурации создайте `JwtDecoder` и объедините `JwtValidators.createDefaultWithIssuer(issuer)` с собственным `OAuth2TokenValidator<Jwt>` для `aud`. Не заменяйте стандартные validators одним кастомным — иначе легко потерять проверку времени жизни или issuer.

## OAuth 2.0, OIDC и проверка JWT

### Разделение ролей протоколов

**OAuth 2.0** делегирует доступ к API. Его участники: Resource Owner, Client, Authorization Server и Resource Server. Access token предназначен API и не обязан быть JWT.

**OpenID Connect (OIDC)** добавляет слой аутентификации поверх OAuth 2.0. ID token сообщает клиенту результат входа и данные о пользователе; он предназначен конкретному client (`aud`) и не должен использоваться как access token к произвольному API.

В Spring Security:

- `oauth2Login()` делает приложение OAuth Client/OIDC Relying Party и входит пользователем через Authorization Code flow;
- `oauth2ResourceServer().jwt()` защищает API Bearer JWT;
- `oauth2ResourceServer().opaqueToken()` проверяет opaque access token через introspection;
- Authorization Server — отдельная роль; её не следует самодельно реализовывать контроллером `/token`.

Для browser/public clients используйте Authorization Code + PKCE. Password Grant и Implicit Grant не следует выбирать для новых систем. Client secret нельзя считать секретом в SPA или мобильном приложении.

### Правила валидации JWT

Resource Server должен проверить весь контракт токена, а не только подпись:

1. разрешённый алгоритм и подпись доверенным ключом;
2. `iss` — ожидаемый Authorization Server;
3. `aud` — этот API, а не другой сервис;
4. `exp`, `nbf` и допустимый clock skew;
5. назначение/тип токена, если провайдер публикует `typ` или отдельный признак;
6. необходимые scopes/roles и бизнес-ограничения.

> Парсинг `header.payload.signature`, вызов низкоуровневого криптографического API или доверие `alg` из входного header — опасная самодельная проверка. Используйте `JwtDecoder`/Nimbus через Resource Server и фиксируйте допустимые algorithms политикой доверия.

Не принимайте ID token вместо access token и токен одного tenant/окружения в другом. Не кладите секретные или чувствительные данные в claims: подписанный JWT обычно читаем, а его копии остаются в логах, прокси и клиентах до истечения срока.

### Refresh-токены и ротация ключей

**Refresh token** предъявляется только Authorization Server для получения нового access token. Resource Server и обычный API endpoint не должны принимать его как Bearer credential. Для browser-приложения безопаснее BFF с `HttpOnly` cookie, чем выдача долгоживущего refresh token JavaScript-коду.

Практика ротации refresh tokens:

- после каждого обмена выдавать новый refresh token, а старый помечать использованным;
- хранить серверное состояние token family и обнаруживать повторное предъявление старого токена;
- при reuse отзывать всю семью и требовать повторный вход;
- задавать абсолютный и idle lifetime, связывать токен с client и субъектом;
- хранить токены защищённо и не писать их в URL или логи.

Ротация **подписывающих ключей** — другая операция. Authorization Server публикует JWKS с `kid`; во время перехода старый и новый public keys доступны одновременно. Сначала публикуют новый ключ, затем начинают им подписывать, а старый удаляют только после истечения всех выпущенных им токенов плюс допустимый clock skew. Resource Server должен обновлять JWKS штатным decoder, кэшировать ключи и корректно обрабатывать неизвестный `kid`. Private key хранится в KMS/HSM/secret manager, но не в Git и не в container image.

## Авторизация: RBAC и method security

При **RBAC** permissions объединяются в роли. В Spring Security `hasRole("ADMIN")` по соглашению проверяет authority `ROLE_ADMIN`, а `hasAuthority("orders:read")` проверяет точную строку. Не добавляйте `ROLE_` в аргумент `hasRole`.

HTTP-правила защищают URL, а method security защищает use case независимо от способа вызова:

```java
@Service
class OrderService {

    @PreAuthorize("hasAuthority('SCOPE_orders.read')")
    Order findById(long id) {
        return loadOrder(id);
    }

    @PreAuthorize("hasRole('ADMIN') or #owner == authentication.name")
    void cancel(long orderId, String owner) {
        // В production owner следует получить из доверенных данных заказа,
        // а не принимать без проверки из HTTP-запроса.
    }
}
```

`@EnableMethodSecurity` включает `@PreAuthorize` и другие method interceptors. Аннотация работает через Spring AOP: self-invocation внутри того же bean обходит proxy. Предпочитайте небольшие permissions (`orders:read`, `orders:cancel`) и отображайте внешние claims на внутреннюю модель централизованно. Не позволяйте клиенту присылать собственные roles.

Для object-level authorization проверяйте принадлежность конкретного ресурса: одной роли `USER` недостаточно, чтобы разрешить `/users/{id}`. Защищайте команды и чтение; фильтрация UI или списка не заменяет проверку объекта на сервере.

## Защита HTTP-границы

### CSRF

**Cross-Site Request Forgery** заставляет браузер отправить изменяющий состояние запрос с credentials жертвы, которые браузер прикладывает автоматически. Spring Security включает CSRF по умолчанию для небезопасных HTTP methods.

- Для MVC forms вставляйте выданный framework CSRF token; Thymeleaf делает это при корректной интеграции.
- Для SPA используйте согласованный repository/header pattern и отправляйте token отдельным header.
- `GET`, `HEAD`, `OPTIONS` и `TRACE` не должны менять состояние.
- `SameSite` — полезный дополнительный слой, но не универсальная замена CSRF token.
- Не отключайте CSRF глобально ради одного webhook; игнорируйте только точно заданный endpoint и защитите его подписью/аутентификацией.

Ошибка CSRF происходит до контроллера и обычно возвращает 403. Наличие JWT не отменяет CSRF, если JWT хранится в cookie.

### CORS

**CORS** — браузерная политика чтения cross-origin ответов, а не аутентификация и не защита от curl/server-to-server запросов. Preflight должен обрабатываться до security, потому что в нём обычно нет cookies. Spring Security интегрируется с Spring MVC через `.cors(Customizer.withDefaults())`.

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

При `allowCredentials(true)` нельзя использовать wildcard origin `*`; перечисляйте доверенные origins. Не отражайте входной `Origin` без allowlist.

### Secure headers

Spring Security по умолчанию добавляет набор защитных headers. Не отключайте `.headers()` целиком. Проверьте под свою архитектуру:

- HSTS включается только для HTTPS-ответов; настроить TLS и корректное распознавание proxy headers нужно на инфраструктурной границе;
- `X-Content-Type-Options: nosniff` препятствует MIME sniffing;
- защита от framing должна быть согласована с требуемыми iframe;
- Content Security Policy обычно задают явно, начиная с report-only и устраняя inline scripts;
- `Referrer-Policy` и `Permissions-Policy` уменьшают утечки и поверхность браузерных API;
- чувствительные ответы должны иметь подходящий `Cache-Control`.

Headers — defense in depth. Они не исправляют XSS, неверную авторизацию или утечку token.

### HTTP 401 и 403


| Код | Смысл | Типичный пример |
|---|---|---|
| `401 Unauthorized` | Аутентификация отсутствует, Bearer token недействителен или истёк | Запрос к защищённому API без токена; Resource Server добавляет `WWW-Authenticate` |
| `403 Forbidden` | Субъект распознан, но ему не хватает полномочий; также возможен отказ CSRF | Пользователь с ролью `USER` вызывает admin endpoint |

Название 401 историческое: по смыслу это «не аутентифицирован». Не маскируйте все ошибки как 401 и не перенаправляйте REST API на HTML login page. Для API настройте согласованный JSON/Problem Details ответ, не раскрывая секретные детали проверки.

## Logout, отзыв и завершение сессий

Для session-based приложения logout должен инвалидировать серверную сессию, очистить security context и cookie. Stateful logout — изменяющая состояние операция, поэтому оставляйте POST и CSRF-защиту; GET logout облегчает CSRF-атаки.

Для self-contained access JWT «удалить токен на клиенте» недостаточно: скопированный токен действителен до `exp`. Используйте комбинацию:

- короткого срока жизни access token;
- отзыва refresh token/token family на Authorization Server;
- интроспекции opaque tokens, если нужен почти немедленный централизованный отзыв;
- denylist по `jti` до `exp` для критических сценариев с пониманием стоимости общего state;
- OIDC RP-Initiated/Back-Channel Logout только если провайдер и требуемая модель сессий это поддерживают.

Компрометация ключа требует аварийной ротации и оценки всех подписанных им токенов. Обычная публикация нового ключа не отзывает старые JWT, пока старый ключ остаётся доверенным.

## Тестирование с spring-security-test

Следующий slice-test проверяет не только happy path, но и границы 401/403. Конфигурация `jwt()` создаёт тестовый `JwtAuthenticationToken`; реальную криптографическую валидацию decoder нужно отдельно проверять интеграционным тестом с тестовым issuer/JWKS.

```java
package example.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(ApiSecurityConfig.class)
class OrderSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/api/orders/42"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutScopeIsForbidden() throws Exception {
        mvc.perform(get("/api/orders/42").with(jwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    void tokenWithScopeCanReadOrder() throws Exception {
        mvc.perform(get("/api/orders/42").with(jwt()
                .jwt(token -> token.subject("alice"))
                .authorities(new SimpleGrantedAuthority("SCOPE_orders.read"))))
            .andExpect(status().isOk());
    }
}
```

Для session/CSRF сценария:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Test
void postWithoutCsrfIsForbidden() throws Exception {
    mvc.perform(post("/profile").with(user("alice")))
        .andExpect(status().isForbidden());
}

@Test
void authenticatedPostWithCsrfSucceeds() throws Exception {
    mvc.perform(post("/profile")
            .with(user("alice").roles("USER"))
            .with(csrf()))
        .andExpect(status().is2xxSuccessful());
}
```

Обязательный набор негативных тестов: anonymous, неверная роль/scope, чужой объект, истёкший/неверный issuer/audience JWT, отсутствующий CSRF token, запрещённый Origin и logout/revocation. Не заменяйте все тесты `@WithMockUser`: он обходит реальный token mapping.

## Типичные ошибки и практический чек-лист


| Ошибка | Риск | Правильный подход |
|---|---|---|
| Самостоятельно разобрать JWT и проверить только подпись | Пропуск `iss`, `aud`, времени, algorithm confusion | Штатный Resource Server и полный набор validators |
| Хранить client secret/private key/token в Git | Неустранимая утечка через историю и forks | Secret manager/KMS, short-lived credentials, немедленная ротация после утечки |
| Отключить CSRF для cookie-аутентификации | Запросы от имени жертвы | Оставить CSRF token; отключать только для действительно stateless Bearer API |
| Разрешить CORS `*` вместе с credentials | Чтение ответов недоверенным origin или сломанная политика | Точный allowlist origins, methods и headers |
| Использовать ID token как API access token | Токен другой аудитории и назначения | API принимает предназначенный ему access token |
| Хранить access/refresh token в `localStorage` без анализа угроз | Кража при XSS | BFF/HttpOnly cookie либо минимальный lifetime и усиленная XSS-защита |
| Полагаться только на URL rules | Обход при другом пути вызова сервиса | Defense in depth: request + method/object authorization |
| Сделать JWT долгоживущим «чтобы не было refresh» | Долгое окно компрометации | Короткий access token, безопасная refresh rotation |
| Вернуть 401 при нехватке роли | Клиент пытается повторно логиниться вместо обработки отказа | 401 для отсутствующей/невалидной identity, 403 для недостаточных прав |

Production-чек-лист:

1. Всё внешнее соединение использует TLS; доверие к forwarded headers ограничено известным proxy.
2. Password encoder и параметры проверены нагрузочным тестом; credentials никогда не логируются.
3. Для JWT зафиксированы issuer, audience и допустимые algorithms, проверены сроки и clock skew.
4. Roles/scopes выдаёт доверенный issuer, mapping покрыт тестами, object-level checks находятся на сервере.
5. CSRF соответствует способу передачи credentials; CORS содержит минимальный allowlist.
6. Cookies имеют `Secure`, `HttpOnly`, подходящий `SameSite`, узкие `Path`/`Domain`.
7. Есть процедуры refresh reuse detection, logout/revocation и штатной/аварийной ротации ключей.
8. Секреты поступают из secret manager, включено secret scanning, утёкшие значения отзываются, а не просто удаляются из последнего commit.
9. Логи и метрики фиксируют результат и причину класса ошибки без token, password и чувствительных claims.
10. Зависимости регулярно обновляются, а негативные security tests выполняются в CI.

## Практическое упражнение

Создайте Spring Boot API каталога заказов:

1. Реализуйте `GET /api/orders/{id}` и `POST /api/orders/{id}/cancel`.
2. Настройте `SecurityFilterChain` как stateless OAuth 2.0 Resource Server.
3. Для чтения требуйте `SCOPE_orders.read`, для отмены — `SCOPE_orders.cancel`; роль `ADMIN` может работать с любым заказом.
4. Обычному пользователю разрешите операции только со своими заказами через `@PreAuthorize` и проверку владельца из БД.
5. Зафиксируйте `issuer` и `audience`; не создавайте собственный фильтр разбора JWT.
6. Добавьте CORS allowlist для одного UI origin и объясните в README упражнения, почему CSRF отключён или оставлен включённым.
7. Напишите MockMvc-тесты для 401, 403, обеих scopes, чужого заказа и admin. Отдельным интеграционным тестом проверьте неверные `iss`, `aud`, подпись и истёкший токен.
8. Опишите стратегию access-token lifetime, refresh rotation, key rotation и экстренного revoke.

**Критерий готовности:** нельзя получить или изменить чужой заказ заменой path variable; тесты различают отсутствие identity и недостаточные permissions; в репозитории нет ключей и token fixtures с реальными секретами.

## Вопросы для самопроверки

1. **Чем `securityMatcher` отличается от `requestMatchers`?**  
   *Ответ:* Первый определяет, применима ли целая `SecurityFilterChain`; вторые выбирают правила авторизации внутри уже выбранной цепочки.

2. **Где находится identity текущего запроса?**  
   *Ответ:* В `Authentication` внутри `SecurityContext`, доступного через интеграцию Spring MVC или `SecurityContextHolder`. Способ сохранения между запросами зависит от session/stateless модели.

3. **Почему для Bearer JWT обычно получают 401, а не redirect на login?**  
   *Ответ:* Resource Server — API, а не интерактивный login client. Отсутствующий или недействительный token запускает `AuthenticationEntryPoint` и ответ 401 с `WWW-Authenticate`.

4. **Когда следует вернуть 403?**  
   *Ответ:* Когда аутентифицированному субъекту не хватает permissions, а также при некоторых отказах до контроллера, например неверном CSRF token.

5. **Достаточно ли проверить подпись JWT?**  
   *Ответ:* Нет. Нужны доверенный algorithm/key, issuer, audience, временные claims, назначение токена и полномочия.

6. **Чем ID token отличается от access token?**  
   *Ответ:* ID token сообщает OAuth Client о факте OIDC-аутентификации; access token предъявляется Resource Server для доступа к API.

7. **Почему JWT в cookie не позволяет автоматически отключить CSRF?**  
   *Ответ:* Браузер автоматически прикладывает cookie к cross-site запросу, то есть сохраняется условие CSRF-атаки независимо от формата значения.

8. **Как безопасно обновлять signing key?**  
   *Ответ:* Заранее опубликовать новый public key с новым `kid`, начать подписывать новым private key, держать старый public key до истечения старых токенов и только потом удалить его.

9. **Что даёт refresh token rotation?**  
   *Ответ:* Каждый обмен инвалидирует старый refresh token; повторное использование позволяет обнаружить кражу и отозвать всю token family.

10. **Почему `@PreAuthorize` может не сработать при self-invocation?**  
    *Ответ:* Method security обычно реализована proxy/interceptor; внутренний вызов метода того же объекта не проходит через proxy.

11. **Как хранить пароли?**  
    *Ответ:* Через адаптивную password KDF и `PasswordEncoder`, с уникальной солью и возможностью миграции параметров; не plaintext, reversible encryption или быстрый общий hash.

12. **Удаляет ли logout уже выпущенный JWT?**  
    *Ответ:* Нет. Self-contained access token остаётся валиден до `exp`, если не применяется denylist или иной серверный механизм; logout должен также отозвать refresh/session state.

---

[← Назад к оглавлению Spring](README.md)
