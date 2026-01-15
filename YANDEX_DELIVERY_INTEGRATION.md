# Интеграция Яндекс Доставки

## Что было сделано

### Backend (orderservice)

1. **Удалены файлы СДЭК:**
   - `CdekProxyController.java` - контроллер для работы с API СДЭК
   - `CdekDeliveryDto.java` - DTO для данных СДЭК

2. **Созданы новые файлы для Яндекс Доставки:**
   - `YandexDeliveryProxyController.java` - контроллер для проксирования запросов к API Яндекс Доставки
   - `YandexDeliveryDto.java` - DTO для данных Яндекс Доставки
   - `YandexDeliveryProperties.java` - конфигурация для API Яндекс Доставки

3. **Обновлены entity и DTO:**
   - `Order.java` - заменены поля CDEK на поля Yandex (yandex_pickup_point_id, yandex_pickup_point_address и т.д.)
   - `OrderRequestDto.java` - заменено поле `cdekDelivery` на `yandexDelivery`
   - `OrderResponseDto.java` - заменены поля CDEK на поля Yandex
   - `OrderService.java` - обновлена логика работы с данными доставки

4. **Миграция БД:**
   - `V3__replace_cdek_with_yandex_delivery.sql` - миграция для замены полей CDEK на Yandex в таблице orders

5. **Конфигурация:**
   - Обновлен `application.yml` с настройками для Яндекс Доставки

### Frontend (client-storefront)

1. **Создан компонент виджета:**
   - `YandexDeliveryWidget.tsx` - React компонент для отображения виджета выбора ПВЗ Яндекс Доставки

2. **Обновлена страница оформления заказа:**
   - `checkout/page.tsx` - интегрирован виджет Яндекс Доставки
   - Добавлена логика выбора ПВЗ
   - Обновлена валидация формы
   - Добавлена передача данных Яндекс Доставки при создании заказа

## Настройка

### Backend

В файле `orderservice/src/main/resources/application.yml` настройте параметры:

```yaml
yandex:
  delivery:
    api-url: https://b2b.taxi.yandex.net/b2b/cargo/integration/v2
    api-token: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
    source-platform-station: 05e809bb-4521-42d9-a936-0fb0744c0fb3
    default-weight: 10000
```

Или через переменные окружения:
- `YANDEX_DELIVERY_API_URL` - URL API Яндекс Доставки
- `YANDEX_DELIVERY_API_TOKEN` - токен для работы с API (уже установлен)
- `YANDEX_DELIVERY_SOURCE_STATION` - ID станции отгрузки (уже установлен)
- `YANDEX_DELIVERY_DEFAULT_WEIGHT` - вес по умолчанию в граммах

### Frontend

Виджет автоматически загружается со скрипта Яндекса:
```
https://yastatic.net/s3/delivery-front/widgets/ya-delivery.js
```

## Использование

### Как это работает:

1. **Выбор метода доставки:**
   - Пользователь выбирает метод доставки на странице checkout
   - Если метод доставки содержит слова "яндекс", "пвз" или "pickup", автоматически показывается виджет

2. **Выбор ПВЗ:**
   - Виджет отображает карту с доступными пунктами выдачи
   - Пользователь выбирает удобный ПВЗ
   - Автоматически рассчитывается стоимость и срок доставки

3. **Создание заказа:**
   - При создании заказа в API передаются все данные о выбранном ПВЗ:
     - ID пункта выдачи
     - Адрес и название ПВЗ
     - Координаты
     - Стоимость и срок доставки
     - График работы и телефон

4. **Хранение данных:**
   - Все данные о Яндекс Доставке сохраняются в таблице `orders`
   - При получении информации о заказе возвращаются полные данные о доставке

## API Endpoints

### Backend

**GET** `/api/yandex-delivery/pickup-points`
- Получение списка ПВЗ по городу или координатам
- Параметры: `city`, `latitude`, `longitude`

**POST** `/api/yandex-delivery/calculate`
- Расчет стоимости и сроков доставки
- Body: JSON с параметрами расчета

**GET** `/api/yandex-delivery/config`
- Получение конфигурации виджета
- Возвращает: `sourcePlatformStation`, `defaultWeight`, `apiAvailable`

**POST** `/api/yandex-delivery/widget-proxy`
- Универсальный прокси для запросов виджета
- Body: JSON с параметрами запроса

## Структура данных

### YandexDeliveryDto (Backend)
```java
{
  "pickupPointId": "string",
  "pickupPointAddress": "string",
  "pickupPointName": "string",
  "latitude": double,
  "longitude": double,
  "deliveryPrice": BigDecimal,
  "deliveryTerm": integer,
  "pickupPointType": "pickup_point" | "terminal",
  "workSchedule": "string",
  "phone": "string"
}
```

### YandexDeliveryData (Frontend)
```typescript
{
  pickupPointId: string;
  pickupPointAddress: string;
  pickupPointName: string;
  latitude: number;
  longitude: number;
  deliveryPrice: number;
  deliveryTerm: number;
  pickupPointType: string;
  workSchedule?: string;
  phone?: string;
}
```

## Миграция данных

При развертывании автоматически выполнится миграция `V3__replace_cdek_with_yandex_delivery.sql`:
- Удалятся старые поля CDEK из таблицы `orders`
- Добавятся новые поля Yandex
- Создастся индекс для оптимизации запросов

**Важно:** Если в БД есть существующие заказы с данными CDEK, они будут потеряны при миграции.

## Дополнительные настройки виджета

В компоненте `YandexDeliveryWidget.tsx` можно настроить:
- `city` - город по умолчанию
- `sourcePlatformStation` - станция отгрузки
- `weight` - вес отправления в граммах
- Фильтры по типам ПВЗ
- Способы оплаты
- Размеры виджета

## Тестирование

1. Запустите orderservice
2. Запустите client-storefront
3. Перейдите на страницу оформления заказа
4. Выберите метод доставки с ПВЗ
5. Выберите пункт выдачи на карте
6. Оформите заказ
7. Проверьте, что данные о доставке сохранились в БД

## Troubleshooting

**Виджет не загружается:**
- Проверьте консоль браузера на наличие ошибок
- Убедитесь, что скрипт Яндекса загружен
- Проверьте, что контейнер с ID `yandex-delivery-widget` существует

**Ошибки API:**
- Проверьте токен API в конфигурации
- Убедитесь, что `source-platform-station` корректен
- Проверьте логи orderservice

**Данные не сохраняются:**
- Убедитесь, что миграция БД выполнена успешно
- Проверьте маппинг в `OrderMapper`
- Проверьте логи при создании заказа
