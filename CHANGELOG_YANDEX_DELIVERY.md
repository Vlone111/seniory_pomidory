# Changelog - Интеграция Яндекс Доставки

**Дата:** 2026-01-15  
**Задача:** Замена СДЭК на Яндекс Доставку с виджетом ПВЗ

---

## Удалено

### Backend
- ❌ `CdekProxyController.java` - контроллер для API СДЭК
- ❌ `CdekDeliveryDto.java` - DTO для данных СДЭК
- ❌ Поля CDEK в таблице `orders` (через миграцию)

---

## Добавлено

### Backend

**Новые файлы:**
- ✅ `YandexDeliveryProxyController.java` - прокси-контроллер для API Яндекс Доставки
  - `GET /api/yandex-delivery/pickup-points` - список ПВЗ
  - `POST /api/yandex-delivery/calculate` - расчет доставки
  - `GET /api/yandex-delivery/config` - конфигурация виджета
  - `POST /api/yandex-delivery/widget-proxy` - универсальный прокси

- ✅ `YandexDeliveryDto.java` - DTO для данных Яндекс Доставки
  - pickupPointId
  - pickupPointAddress
  - pickupPointName
  - latitude/longitude
  - deliveryPrice/deliveryTerm
  - pickupPointType
  - workSchedule/phone

- ✅ `YandexDeliveryProperties.java` - конфигурация API
  - apiUrl
  - apiToken (уже установлен)
  - sourcePlatformStation
  - defaultWeight

**Миграции БД:**
- ✅ `V3__replace_cdek_with_yandex_delivery.sql`
  - Удаление полей CDEK
  - Добавление полей Yandex
  - Индексы для оптимизации

- ✅ `V4__add_yandex_delivery_method.sql`
  - Добавление поля `description` в `delivery_methods`
  - Создание метода доставки "Яндекс Доставка (ПВЗ)"
  - Добавление описаний для существующих методов

**Обновления в существующих файлах:**
- ✅ `Order.java`
  - Заменены поля cdek_* на yandex_*
  - 10 новых полей для Яндекс Доставки

- ✅ `OrderRequestDto.java`
  - Заменено `cdekDelivery` на `yandexDelivery`

- ✅ `OrderResponseDto.java`
  - Заменены поля CDEK на Yandex

- ✅ `OrderService.java`
  - Обновлена логика работы с данными доставки
  - Использование Yandex delivery price

- ✅ `DeliveryMethod.java`
  - Добавлено поле `description`

- ✅ `DeliveryMethodDto.java`
  - Добавлено поле `description`

- ✅ `application.yml`
  - Добавлена секция `yandex.delivery` с настройками

### Frontend

**Новые файлы:**
- ✅ `components/delivery/YandexDeliveryWidget.tsx`
  - React компонент виджета
  - Автоматическая загрузка скрипта Яндекса
  - Обработка выбора ПВЗ
  - Отображение выбранного пункта

**Обновления в существующих файлах:**
- ✅ `app/checkout/page.tsx`
  - Импорт YandexDeliveryWidget
  - Состояние yandexDeliveryData
  - Условное отображение виджета
  - Обновленная валидация формы
  - Передача данных ПВЗ при создании заказа
  - Динамическое отображение стоимости доставки

---

## Конфигурация

**Backend (`application.yml`):**
```yaml
yandex:
  delivery:
    api-token: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
    source-platform-station: 05e809bb-4521-42d9-a936-0fb0744c0fb3
    default-weight: 10000
```

**Frontend:**
- Скрипт виджета: `https://yastatic.net/s3/delivery-front/widgets/ya-delivery.js`
- Автоматическое определение метода доставки по ключевым словам

---

## Новые возможности

1. **Выбор ПВЗ на карте**
   - Интерактивная карта с пунктами выдачи
   - Фильтрация по типу ПВЗ
   - Отображение графика работы и контактов

2. **Автоматический расчет стоимости**
   - Стоимость рассчитывается для выбранного ПВЗ
   - Срок доставки отображается автоматически

3. **Сохранение данных ПВЗ**
   - Полная информация о выбранном ПВЗ в заказе
   - Координаты для интеграции с другими сервисами

4. **Валидация**
   - Обязательный выбор ПВЗ для соответствующих методов доставки
   - Проверка корректности данных перед отправкой

---

## Структура БД

**Таблица `orders` - новые поля:**
```sql
yandex_pickup_point_id VARCHAR(100)
yandex_pickup_point_address VARCHAR(500)
yandex_pickup_point_name VARCHAR(255)
yandex_latitude DOUBLE PRECISION
yandex_longitude DOUBLE PRECISION
yandex_delivery_price DECIMAL(10, 2)
yandex_delivery_term INTEGER
yandex_pickup_point_type VARCHAR(50)
yandex_work_schedule VARCHAR(500)
yandex_phone VARCHAR(50)
```

**Таблица `delivery_methods` - новое поле:**
```sql
description VARCHAR(500)
```

---

## Как использовать

1. **Создайте/обновите метод доставки** с названием содержащим:
   - "Яндекс" или "яндекс"
   - "ПВЗ" или "пвз"
   - "pickup" или "Pickup"

2. **На странице checkout:**
   - Выберите этот метод доставки
   - Виджет появится автоматически
   - Выберите ПВЗ на карте
   - Оформите заказ

3. **Данные сохраняются автоматически** в таблице orders

---

## Тестирование

**Чек-лист:**
- ✅ Миграция БД выполняется без ошибок
- ✅ Backend запускается успешно
- ✅ Frontend компилируется без ошибок
- ✅ Виджет загружается на странице checkout
- ✅ ПВЗ выбирается корректно
- ✅ Стоимость и срок отображаются правильно
- ✅ Заказ создается с данными Яндекс Доставки
- ✅ Данные сохраняются в БД

---

## Документация

- `YANDEX_DELIVERY_INTEGRATION.md` - полная техническая документация
- `QUICK_START_YANDEX_DELIVERY.md` - быстрый старт

---

## Версии

**Backend:**
- Spring Boot: (из проекта)
- PostgreSQL: (из проекта)
- Flyway: для миграций

**Frontend:**
- React/Next.js: (из проекта)
- Яндекс Доставка виджет: latest

---

## Авторы

Интеграция выполнена: Claude (AI Assistant)  
Дата: 15 января 2026
