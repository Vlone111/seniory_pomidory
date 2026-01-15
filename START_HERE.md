# 🚀 START HERE - Яндекс Доставка

## ✅ Что сделано

- ✅ Удалена интеграция СДЭК  
- ✅ Добавлена интеграция Яндекс Доставки с виджетом ПВЗ  
- ✅ Создан контроллер API для Яндекс Доставки  
- ✅ Обновлена база данных (автоматическая миграция)  
- ✅ Создан компонент виджета для фронтенда  
- ✅ Настроен Docker Compose  
- ✅ Токен API уже установлен

---

## 🎯 Запуск через Docker (РЕКОМЕНДУЕТСЯ)

### Вариант 1: Автоматический запуск и тестирование

```bash
cd /Users/evgenijslavkin/Desktop/seniory_pomidory
./build-and-test.sh
```

**Этот скрипт:**
- Соберет образы
- Запустит всю инфраструктуру
- Протестирует API
- Проверит миграции БД
- Покажет интерактивное меню

⏱️ **Время:** ~10 минут при первом запуске (Maven загружает зависимости)

### Вариант 2: Быстрый тест (только backend)

```bash
cd /Users/evgenijslavkin/Desktop/seniory_pomidory
./quick-test.sh
```

⏱️ **Время:** ~2 минуты

### Вариант 3: Ручной запуск

```bash
cd /Users/evgenijslavkin/Desktop/seniory_pomidory

# Остановить старые контейнеры
docker-compose down

# Собрать образы (первый раз ~10 минут)
docker-compose build orderservice client-storefront

# Запустить всю систему
docker-compose up -d

# Посмотреть логи
docker-compose logs -f orderservice
```

---

## 🌐 После запуска

Откройте в браузере:

1. **Client Storefront (покупатель):**  
   http://localhost:3001

2. **OrderService API:**  
   http://localhost:8087/api/yandex-delivery/config

3. **RabbitMQ Management:**  
   http://localhost:15672 (admin/admin)

---

## 🧪 Тестирование виджета Яндекс Доставки

1. Откройте http://localhost:3001
2. Добавьте товары в корзину
3. Перейдите к оформлению заказа (Checkout)
4. Выберите метод доставки "Яндекс Доставка (ПВЗ)"
5. На карте выберите пункт выдачи заказа
6. Проверьте что стоимость и срок отображаются
7. Оформите заказ
8. Проверьте что данные сохранились в БД:

```bash
docker-compose exec db psql -U postgres -d orderdb
\d orders
SELECT * FROM delivery_methods;
```

---

## 📊 Проверка статуса

### Проверить что все работает

```bash
# Статус контейнеров
docker-compose ps

# Логи orderservice
docker-compose logs orderservice

# Логи client-storefront  
docker-compose logs client-storefront

# Проверка API
curl http://localhost:8087/api/yandex-delivery/config
curl http://localhost:8087/api/delivery-methods
```

### Проверка БД

```bash
# Подключение к БД
docker-compose exec db psql -U postgres -d orderdb

# Проверка полей Yandex в таблице orders
\d orders

# Проверка методов доставки
SELECT * FROM delivery_methods;

# Выход из psql
\q
```

---

## 🐛 Если что-то не работает

### OrderService не запускается

```bash
# Логи
docker-compose logs orderservice

# Пересоздать
docker-compose up -d --force-recreate orderservice
```

### Ошибка "порт занят"

```bash
# Остановить все
docker-compose down

# Проверить что порты свободны
lsof -i :8087  # orderservice
lsof -i :3001  # client-storefront
lsof -i :5432  # postgres
```

### Ошибки миграции БД

```bash
# Полная очистка и перезапуск
docker-compose down -v
docker-compose up -d db
sleep 15
docker-compose up -d orderservice
```

### Сборка долго идет

Это нормально при первом запуске! Maven загружает все зависимости.

Следите за прогрессом:
```bash
tail -f /tmp/orderservice-build.log
```

---

## 📚 Документация

- `DOCKER_BUILD_GUIDE.md` - полное руководство по Docker
- `YANDEX_DELIVERY_INTEGRATION.md` - техническая документация
- `QUICK_START_YANDEX_DELIVERY.md` - быстрый старт без Docker
- `CHANGELOG_YANDEX_DELIVERY.md` - список изменений

---

## 🎁 Бонус: Полезные команды

```bash
# Остановить все
docker-compose down

# Остановить и удалить все данные
docker-compose down -v

# Перезапустить сервис
docker-compose restart orderservice

# Логи в реальном времени
docker-compose logs -f

# Зайти в контейнер
docker-compose exec orderservice sh

# Проверка health
curl http://localhost:8087/actuator/health
```

---

## 💡 Структура API

### Yandex Delivery Endpoints (OrderService)

- `GET /api/yandex-delivery/config` - конфигурация виджета
- `GET /api/yandex-delivery/pickup-points` - список ПВЗ
- `POST /api/yandex-delivery/calculate` - расчет доставки
- `POST /api/yandex-delivery/widget-proxy` - прокси для виджета

### Order Endpoints

- `GET /api/delivery-methods` - методы доставки
- `GET /api/payment-methods` - методы оплаты
- `POST /api/orders` - создание заказа
- `GET /api/orders/{id}` - получение заказа

---

## ⚙️ Конфигурация

Все настройки уже в `docker-compose.yml`:

```yaml
YANDEX_DELIVERY_API_TOKEN: y0__xCd-KvSCBix9Bwgp8OMiRapyG8u0V5lGR_NrN_ay2g-CTuTww
YANDEX_DELIVERY_SOURCE_STATION: 05e809bb-4521-42d9-a936-0fb0744c0fb3
YANDEX_DELIVERY_DEFAULT_WEIGHT: 10000
```

Ничего менять не нужно!

---

## 🎉 Готово!

После успешного запуска:

1. ✅ OrderService работает на порту 8087
2. ✅ Client Storefront работает на порту 3001
3. ✅ Виджет Яндекс Доставки интегрирован
4. ✅ Миграции БД выполнены
5. ✅ API готов к использованию

**Следующий шаг:** Откройте http://localhost:3001 и протестируйте!

---

**Вопросы?** Проверьте документацию в файлах выше или посмотрите логи.
