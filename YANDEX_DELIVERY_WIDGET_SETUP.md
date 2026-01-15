# Интеграция виджета Яндекс.Доставки

## Описание

В проект внедрен виджет Яндекс.Доставки для выбора пунктов выдачи заказов (ПВЗ) на карте. Виджет интегрирован в процесс оформления заказа (checkout) и позволяет покупателям выбирать удобное место получения товара.

## API токен

API токен уже настроен в конфигурации:
```
y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
```

## Архитектура интеграции

### Backend (orderservice)

#### 1. Конфигурация (`YandexDeliveryProperties.java`)
```java
yandex:
  delivery:
    api-url: https://b2b.taxi.yandex.net/b2b/cargo/integration/v2
    api-token: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
    source-platform-station: 05e809bb-4521-42d9-a936-0fb0744c0fb3
    default-weight: 10000
```

#### 2. Прокси-контроллер (`YandexDeliveryProxyController.java`)

Предоставляет следующие API endpoints:

- **GET** `/api/yandex-delivery/config` - Получение конфигурации для виджета
- **GET** `/api/yandex-delivery/pickup-points` - Список пунктов выдачи по городу/координатам
- **POST** `/api/yandex-delivery/calculate` - Расчет стоимости и сроков доставки
- **POST** `/api/yandex-delivery/widget-proxy` - Универсальный прокси для виджета

#### 3. DTO классы

**YandexDeliveryDto.java** - содержит данные о выбранном ПВЗ:
- `pickupPointId` - ID пункта выдачи
- `pickupPointAddress` - Адрес ПВЗ
- `pickupPointName` - Название ПВЗ
- `latitude/longitude` - Координаты
- `deliveryPrice` - Стоимость доставки
- `deliveryTerm` - Срок доставки в днях
- `pickupPointType` - Тип (pickup_point/terminal)

**OrderRequestDto.java** - обновлен для включения данных о Яндекс.Доставке:
```java
private YandexDeliveryDto yandexDelivery;
```

### Frontend (client-storefront)

#### 1. Компонент виджета (`YandexDeliveryWidget.tsx`)

Расположение: `/client-storefront/src/components/delivery/YandexDeliveryWidget.tsx`

**Props:**
```typescript
{
  city?: string;                      // Город для отображения (по умолчанию: "Москва")
  sourcePlatformStation?: string;     // ID станции отгрузки
  weight?: number;                    // Вес отправления в граммах
  onSelectPoint?: (point: any) => void; // Callback при выборе ПВЗ
  className?: string;                 // CSS классы
}
```

**Особенности:**
- Автоматически загружает скрипт виджета Яндекс.Доставки
- Получает конфигурацию из бэкенда
- Отображает загрузку и ошибки
- Показывает выбранный пункт выдачи с деталями

#### 2. Сервис API (`yandex-delivery.service.ts`)

Расположение: `/client-storefront/src/services/yandex-delivery.service.ts`

**Методы:**
- `getConfig()` - Получение конфигурации
- `getPickupPoints()` - Получение списка ПВЗ
- `calculateDelivery()` - Расчет доставки
- `widgetProxy()` - Универсальный прокси

#### 3. Интеграция в checkout (`checkout/page.tsx`)

Виджет интегрирован в страницу оформления заказа после секции выбора способа доставки.

**Функционал:**
- Автоматическая передача города из адреса
- Сохранение выбранного ПВЗ в состоянии
- Передача данных ПВЗ при создании заказа
- Отображение информации о выбранном пункте

## Параметры виджета

### Основные параметры

```javascript
{
  city: "Москва",                          // Город на карте
  size: {
    height: "450px",                       // Высота виджета
    width: "100%"                          // Ширина виджета
  },
  source_platform_station: "05e809bb...", // Станция отгрузки
  physical_dims_weight_gross: 10000,      // Вес (граммы)
  delivery_price: (price) => price + " руб",
  delivery_term: 3,                       // Срок доставки (дни)
  show_select_button: true,               // Кнопка выбора ПВЗ
}
```

### Фильтры

```javascript
filter: {
  type: [
    "pickup_point",    // Пункт выдачи заказа
    "terminal"         // Постамат
  ],
  is_yandex_branded: false,
  payment_methods: [
    "already_paid",    // Предоплата
    "card_on_receipt"  // Оплата при получении
  ],
  payment_methods_filter: "or"
}
```

## Использование

### 1. Запуск проекта

```bash
# Запуск бэкенда
cd orderservice
./gradlew bootRun

# Запуск фронтенда
cd client-storefront
npm install
npm run dev
```

### 2. Тестирование виджета

1. Откройте страницу: `http://localhost:3000/checkout`
2. Заполните данные получателя
3. В секции "Способ доставки" выберите метод доставки
4. Виджет Яндекс.Доставки отобразится ниже
5. Выберите ПВЗ на карте
6. Информация о выбранном ПВЗ отобразится под картой
7. При оформлении заказа данные ПВЗ будут добавлены в заказ

### 3. Проверка данных

После создания заказа проверьте:
- Адрес доставки обновлен на адрес ПВЗ
- В комментарии к заказу добавлена информация о ПВЗ
- В БД сохранены данные `yandexDelivery`

## Конфигурация

### Переменные окружения (Backend)

```yaml
# application.yml
yandex:
  delivery:
    api-url: ${YANDEX_DELIVERY_API_URL:https://b2b.taxi.yandex.net/b2b/cargo/integration/v2}
    api-token: ${YANDEX_DELIVERY_API_TOKEN:your_token_here}
    source-platform-station: ${YANDEX_DELIVERY_SOURCE_STATION:your_station_id}
    default-weight: ${YANDEX_DELIVERY_DEFAULT_WEIGHT:10000}
```

### Docker Compose

Убедитесь, что в `docker-compose.yml` добавлены переменные:

```yaml
orderservice:
  environment:
    YANDEX_DELIVERY_API_TOKEN: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
    YANDEX_DELIVERY_SOURCE_STATION: 05e809bb-4521-42d9-a936-0fb0744c0fb3
```

## Troubleshooting

### Виджет не загружается

1. Проверьте консоль браузера на ошибки загрузки скрипта
2. Убедитесь, что доступен URL: `https://yastatic.net/s3/taxi-delivery-front/widget/v1.0.0/widget.js`
3. Проверьте, что бэкенд возвращает конфигурацию: `GET /api/yandex-delivery/config`

### Ошибки API

1. Проверьте логи orderservice
2. Убедитесь, что API токен корректен
3. Проверьте доступность API Яндекс.Доставки
4. Убедитесь, что CORS настроен правильно (`@CrossOrigin`)

### ПВЗ не отображаются

1. Проверьте правильность `source_platform_station`
2. Убедитесь, что вес груза указан корректно
3. Проверьте фильтры в конфигурации виджета

## API Reference

### Yandex Delivery API

Документация: https://yandex.ru/dev/taxi/doc/cargo-api/

Основные endpoints:
- `/delivery-options` - Получение вариантов доставки
- `/calculate-delivery` - Расчет стоимости

### Наш прокси API

Base URL: `/api/yandex-delivery`

**GET /config**
```json
{
  "sourcePlatformStation": "05e809bb-4521-42d9-a936-0fb0744c0fb3",
  "defaultWeight": 10000,
  "apiAvailable": true
}
```

**GET /pickup-points?city=Москва**
```json
[
  {
    "id": "pvz_123",
    "address": "ул. Ленина, 1",
    "city": "Москва",
    "price": 250,
    "deliveryTerm": 3
  }
]
```

**POST /calculate**
```json
{
  "weight": 10000,
  "fromLocation": { "latitude": 55.75, "longitude": 37.62 },
  "toLocation": { "latitude": 55.76, "longitude": 37.63 }
}
```

## Дальнейшее развитие

### Возможные улучшения:

1. **Кэширование ПВЗ** - сохранение списка ПВЗ в Redis
2. **Аналитика** - отслеживание выбора ПВЗ
3. **Персонализация** - запоминание последних выбранных ПВЗ
4. **Расширенные фильтры** - добавление фильтров по времени работы, услугам
5. **Мультиязычность** - поддержка других языков
6. **Оффлайн режим** - fallback при недоступности API

### TODO:

- [ ] Добавить unit тесты для компонента виджета
- [ ] Добавить integration тесты для прокси-контроллера
- [ ] Настроить мониторинг API запросов к Яндекс.Доставке
- [ ] Документировать формат данных ПВЗ в БД
- [ ] Добавить админ-панель для управления конфигурацией

## Контакты и поддержка

При возникновении вопросов:
1. Проверьте логи orderservice
2. Проверьте консоль браузера
3. Обратитесь к документации Яндекс.Доставки

---

**Версия:** 1.0
**Дата:** 2026-01-15
**Статус:** ✅ Интегрировано и готово к использованию
