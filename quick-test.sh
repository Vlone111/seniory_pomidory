#!/bin/bash

# Быстрый тест - только orderservice и необходимая инфраструктура

set -e

echo "🚀 Быстрый тест Яндекс Доставки"
echo ""

# Остановка старых контейнеров
echo "Остановка старых контейнеров..."
docker-compose down

# Запуск только необходимых сервисов
echo "Запуск БД, RabbitMQ и OrderService..."
docker-compose up -d db rabbitmq orderservice

echo ""
echo "Ожидание запуска сервисов (45 секунд)..."
sleep 45

echo ""
echo "📋 Проверка сервисов:"
echo ""

# Проверка БД
if docker-compose exec -T db pg_isready -U postgres > /dev/null 2>&1; then
    echo "✓ PostgreSQL работает"
else
    echo "✗ PostgreSQL не работает"
fi

# Проверка OrderService
if curl -s http://localhost:8087/actuator/health > /dev/null 2>&1; then
    echo "✓ OrderService работает"
else
    echo "✗ OrderService не работает"
fi

# Проверка Yandex Delivery API
echo ""
echo "🧪 Тестирование Yandex Delivery API:"
echo ""

echo "1. Конфигурация виджета:"
curl -s http://localhost:8087/api/yandex-delivery/config | jq . || curl -s http://localhost:8087/api/yandex-delivery/config

echo ""
echo ""
echo "2. Методы доставки:"
curl -s http://localhost:8087/api/delivery-methods | jq '.[].name' || curl -s http://localhost:8087/api/delivery-methods

echo ""
echo ""
echo "3. Проверка полей Yandex в БД:"
docker-compose exec -T db psql -U postgres -d orderdb -c "SELECT column_name FROM information_schema.columns WHERE table_name = 'orders' AND column_name LIKE 'yandex%';"

echo ""
echo "✅ Быстрый тест завершен!"
echo ""
echo "Логи orderservice:"
echo "  docker-compose logs orderservice"
echo ""
echo "Остановить:"
echo "  docker-compose down"
