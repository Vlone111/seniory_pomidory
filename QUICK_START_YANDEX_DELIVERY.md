# Быстрый старт - Яндекс Доставка

## Что было сделано

✅ Удалена интеграция СДЭК  
✅ Добавлена интеграция Яндекс Доставки с виджетом ПВЗ  
✅ Обновлена база данных (автоматическая миграция)  
✅ Настроен токен API: `y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww`

## Запуск

### 1. Backend (orderservice)

```bash
cd orderservice
# Конфигурация уже обновлена в application.yml
# При запуске автоматически выполнится миграция БД
./mvnw spring-boot:run
```

### 2. Frontend (client-storefront)

```bash
cd client-storefront
npm install  # если еще не установлены зависимости
npm run dev
```

### 3. Тестирование

1. Откройте http://localhost:3000 (или ваш порт фронтенда)
2. Добавьте товары в корзину
3. Перейдите к оформлению заказа
4. Выберите метод доставки с ПВЗ (pickup/яндекс)
5. На карте выберите пункт выдачи заказа
6. Оформите заказ

## Изменения в файлах

### Backend
- ✅ Удалены: `CdekProxyController.java`, `CdekDeliveryDto.java`
- ✅ Созданы: `YandexDeliveryProxyController.java`, `YandexDeliveryDto.java`, `YandexDeliveryProperties.java`
- ✅ Обновлены: `Order.java`, `OrderService.java`, `OrderRequestDto.java`, `OrderResponseDto.java`
- ✅ Миграция: `V3__replace_cdek_with_yandex_delivery.sql`
- ✅ Конфигурация: `application.yml` (добавлены настройки Яндекс Доставки)

### Frontend
- ✅ Создан: `YandexDeliveryWidget.tsx` - компонент виджета
- ✅ Обновлен: `checkout/page.tsx` - интеграция виджета

## API Endpoints

**GET** `/api/yandex-delivery/config` - конфигурация виджета  
**GET** `/api/yandex-delivery/pickup-points` - список ПВЗ  
**POST** `/api/yandex-delivery/calculate` - расчет доставки  
**POST** `/api/yandex-delivery/widget-proxy` - прокси для виджета

## Структура данных заказа

При создании заказа с Яндекс Доставкой в JSON добавляется поле:

```json
{
  "yandexDelivery": {
    "pickupPointId": "string",
    "pickupPointAddress": "string",
    "pickupPointName": "string",
    "latitude": 0.0,
    "longitude": 0.0,
    "deliveryPrice": 0.0,
    "deliveryTerm": 0,
    "pickupPointType": "pickup_point",
    "workSchedule": "string",
    "phone": "string"
  }
}
```

## Настройка метода доставки

Чтобы виджет Яндекс отображался, название метода доставки должно содержать одно из слов:
- "яндекс"
- "пвз"
- "pickup"

Например: "Яндекс.Доставка (ПВЗ)", "Самовывоз (Pickup Point)", "ПВЗ Яндекс"

## Конфигурация (уже настроено)

В `application.yml`:
```yaml
yandex:
  delivery:
    api-token: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
    source-platform-station: 05e809bb-4521-42d9-a936-0fb0744c0fb3
    default-weight: 10000
```

## Возможные проблемы

**Виджет не загружается:**
- Проверьте консоль браузера
- Убедитесь, что скрипт Яндекса доступен: `https://yastatic.net/s3/delivery-front/widgets/ya-delivery.js`

**Ошибка при создании заказа:**
- Проверьте, что миграция БД выполнена успешно
- Проверьте логи orderservice

**Не сохраняются данные ПВЗ:**
- Убедитесь, что выбран метод доставки с ключевым словом (яндекс/пвз/pickup)
- Проверьте, что ПВЗ выбран на карте (должно появиться зеленое уведомление)

## Подробная документация

См. `YANDEX_DELIVERY_INTEGRATION.md` для полной технической документации.
