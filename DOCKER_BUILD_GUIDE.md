# Docker Build & Test Guide - Яндекс Доставка

## 🚀 Быстрый старт

### Вариант 1: Полная сборка и тестирование (рекомендуется)

```bash
./build-and-test.sh
```

Этот скрипт:
- ✅ Остановит и очистит старые контейнеры
- ✅ Соберет образы orderservice и client-storefront
- ✅ Запустит всю инфраструктуру (БД, RabbitMQ, Redis)
- ✅ Запустит orderservice с интеграцией Яндекс Доставки
- ✅ Запустит client-storefront
- ✅ Протестирует все API endpoints
- ✅ Проверит миграции БД
- ✅ Покажет интерактивное меню для управления

### Вариант 2: Быстрый тест (только backend)

```bash
./quick-test.sh
```

Этот скрипт запускает только:
- БД (PostgreSQL)
- RabbitMQ
- OrderService

И тестирует:
- API Яндекс Доставки
- Методы доставки и оплаты
- Миграции БД

---

## 📦 Ручная сборка

### 1. Сборка образов

```bash
# Только OrderService
docker-compose build orderservice

# Только Client Storefront
docker-compose build client-storefront

# Все сервисы
docker-compose build
```

### 2. Запуск сервисов

```bash
# Вся система
docker-compose up -d

# Только необходимые для тестирования Яндекс Доставки
docker-compose up -d db rabbitmq orderservice client-storefront

# С логами
docker-compose up db rabbitmq orderservice client-storefront
```

### 3. Остановка

```bash
# Остановить все
docker-compose down

# Остановить и удалить volumes (очистка БД)
docker-compose down -v
```

---

## 🧪 Тестирование

### Проверка работы OrderService

```bash
# Health check
curl http://localhost:8087/actuator/health

# Конфигурация Яндекс Доставки
curl http://localhost:8087/api/yandex-delivery/config

# Методы доставки
curl http://localhost:8087/api/delivery-methods

# Методы оплаты
curl http://localhost:8087/api/payment-methods
```

### Проверка Frontend

```bash
# Откройте в браузере
open http://localhost:3001

# Или проверьте доступность
curl -I http://localhost:3001
```

### Проверка БД

```bash
# Подключение к БД
docker-compose exec db psql -U postgres -d orderdb

# Проверка полей Yandex
docker-compose exec db psql -U postgres -d orderdb -c "\d orders"

# Проверка методов доставки
docker-compose exec db psql -U postgres -d orderdb -c "SELECT * FROM delivery_methods;"
```

---

## 📋 Логи

```bash
# Все логи
docker-compose logs

# Логи конкретного сервиса
docker-compose logs orderservice
docker-compose logs client-storefront

# Последние 100 строк с follow
docker-compose logs -f --tail=100 orderservice

# Логи всех сервисов в реальном времени
docker-compose logs -f
```

---

## 🔧 Переменные окружения

### OrderService

В `docker-compose.yml` уже настроены:

```yaml
YANDEX_DELIVERY_API_TOKEN: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
YANDEX_DELIVERY_SOURCE_STATION: 05e809bb-4521-42d9-a936-0fb0744c0fb3
YANDEX_DELIVERY_DEFAULT_WEIGHT: 10000
```

### Client Storefront

```yaml
NODE_ENV: development
DOCKER_ENV: true
NEXT_PUBLIC_API_URL: (пусто - будет использовать относительные пути)
NEXT_PUBLIC_SHOP_DOMAIN: fashion-store
```

---

## 🐛 Troubleshooting

### OrderService не запускается

```bash
# Проверить логи
docker-compose logs orderservice

# Проверить что БД готова
docker-compose exec db pg_isready -U postgres

# Пересоздать контейнер
docker-compose up -d --force-recreate orderservice
```

### Ошибки миграции БД

```bash
# Очистить БД и пересоздать
docker-compose down -v
docker-compose up -d db
sleep 15
docker-compose up -d orderservice
```

### Client Storefront не компилируется

```bash
# Проверить логи
docker-compose logs client-storefront

# Пересобрать образ
docker-compose build --no-cache client-storefront
docker-compose up -d client-storefront
```

### "Cannot connect to Docker daemon"

```bash
# Проверить что Docker запущен
docker ps

# Запустить Docker Desktop (macOS)
open -a Docker
```

---

## 📊 Архитектура системы

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Storefront                        │
│                    (Next.js, Port 3001)                      │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │        YandexDeliveryWidget.tsx                        │ │
│  │  - Загрузка виджета Яндекс                            │ │
│  │  - Выбор ПВЗ на карте                                 │ │
│  │  - Отображение стоимости и срока                      │ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      OrderService                            │
│                   (Spring Boot, Port 8087)                   │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │    YandexDeliveryProxyController                       │ │
│  │  /api/yandex-delivery/config                          │ │
│  │  /api/yandex-delivery/pickup-points                   │ │
│  │  /api/yandex-delivery/calculate                       │ │
│  │  /api/yandex-delivery/widget-proxy                    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         OrderService                                   │ │
│  │  - Создание заказов                                   │ │
│  │  - Сохранение данных ПВЗ                             │ │
│  │  - Расчет стоимости с учетом Яндекс                  │ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL (Port 5432)                    │
│                                                              │
│  orders table:                                              │
│  - yandex_pickup_point_id                                   │
│  - yandex_pickup_point_address                              │
│  - yandex_pickup_point_name                                 │
│  - yandex_latitude / yandex_longitude                       │
│  - yandex_delivery_price                                    │
│  - yandex_delivery_term                                     │
│  - yandex_pickup_point_type                                 │
│  - yandex_work_schedule                                     │
│  - yandex_phone                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Чек-лист тестирования

- [ ] OrderService запустился без ошибок
- [ ] Миграции БД выполнились успешно
- [ ] В таблице `orders` есть поля `yandex_*`
- [ ] В таблице `delivery_methods` есть "Яндекс Доставка (ПВЗ)"
- [ ] API `/api/yandex-delivery/config` возвращает конфигурацию
- [ ] API `/api/delivery-methods` возвращает методы доставки
- [ ] Client Storefront запустился
- [ ] На странице http://localhost:3001 открывается сайт
- [ ] На странице оформления заказа виджет Яндекс загружается
- [ ] При выборе ПВЗ данные сохраняются
- [ ] Заказ создается с данными Яндекс Доставки

---

## 📚 Дополнительные ресурсы

- `YANDEX_DELIVERY_INTEGRATION.md` - полная техническая документация
- `QUICK_START_YANDEX_DELIVERY.md` - быстрый старт без Docker
- `CHANGELOG_YANDEX_DELIVERY.md` - список всех изменений

---

## 🎉 После успешного запуска

Доступные сервисы:
- **Client Storefront**: http://localhost:3001
- **OrderService API**: http://localhost:8087
- **OrderService Health**: http://localhost:8087/actuator/health
- **Yandex Config**: http://localhost:8087/api/yandex-delivery/config
- **PostgreSQL**: localhost:5432 (postgres/postgres)
- **RabbitMQ**: http://localhost:15672 (admin/admin)
- **Grafana**: http://localhost:3000 (admin/admin)

Следующие шаги:
1. Откройте http://localhost:3001
2. Добавьте товары в корзину
3. Перейдите к оформлению заказа
4. Выберите "Яндекс Доставка (ПВЗ)"
5. Выберите ПВЗ на карте
6. Оформите заказ
7. Проверьте в БД что данные сохранились
