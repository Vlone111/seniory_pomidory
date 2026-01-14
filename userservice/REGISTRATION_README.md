# Регистрация с выбором роли

## Описание

При регистрации пользователь может выбрать роль: **user** (обычный пользователь) или **owner** (владелец магазина).

## API

### Регистрация

```
POST /register
Content-Type: application/json

{
  "email": "user@example.com",
  "firstname": "Иван",
  "lastname": "Иванов",
  "password": "password123",
  "role": "user"  // или "owner"
}
```

**Поля:**
- `email` - email пользователя (обязательно)
- `firstname` - имя (обязательно)
- `lastname` - фамилия (обязательно)
- `password` - пароль (обязательно)
- `role` - роль: `"user"` или `"owner"` (опционально, по умолчанию `"user"`)

**Ответ:**
- `200 OK` - пользователь успешно зарегистрирован
- `400 Bad Request` - ошибка валидации или создания пользователя

## Использование на фронтенде

Создайте две кнопки регистрации:

1. **"Зарегистрироваться как пользователь"**
   ```javascript
   fetch('/register', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({
       email: email,
       firstname: firstname,
       lastname: lastname,
       password: password,
       role: 'user'
     })
   })
   ```

2. **"Зарегистрироваться как владелец"**
   ```javascript
   fetch('/register', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({
       email: email,
       firstname: firstname,
       lastname: lastname,
       password: password,
       role: 'owner'
     })
   })
   ```

## Парсинг JWT в KrakenD

KrakenD автоматически парсит JWT токен из заголовка `Authorization: Bearer <token>` и добавляет в запросы следующие заголовки:

- `X-User-Id` - ID пользователя (из поля `sub` в JWT)
- `X-User-Role` - роль пользователя (из `realm_access.roles`, приоритет: admin > owner > user)

Эти заголовки автоматически передаются во все backend сервисы через KrakenD.

## Пример использования заголовков в сервисах

```java
@PostMapping("/some-endpoint")
public ResponseEntity<?> someEndpoint(
    @RequestHeader("X-User-Id") String userId,
    @RequestHeader("X-User-Role") String role) {
    // userId и role автоматически извлекаются из заголовков
    // которые добавил KrakenD после парсинга JWT
}
```





