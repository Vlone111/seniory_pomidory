#!/bin/bash

# Скрипт для билда и тестирования всей системы через Docker Compose

set -e  # Выход при ошибке

echo "=========================================="
echo "🚀 Сборка и тестирование Яндекс Доставки"
echo "=========================================="
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для красивого вывода
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 1. Остановка и очистка старых контейнеров
log_info "Остановка старых контейнеров..."
docker-compose down -v || true

# 2. Удаление старых образов orderservice и client-storefront
log_info "Удаление старых образов..."
docker rmi seniory_pomidory-orderservice || true
docker rmi seniory_pomidory-client-storefront || true

# 3. Сборка образов
log_info "Сборка образов..."
echo ""
log_info "  → Сборка orderservice (с интеграцией Яндекс Доставки)..."
docker-compose build orderservice

log_info "  → Сборка client-storefront..."
docker-compose build client-storefront

# 4. Запуск инфраструктуры
log_info "Запуск инфраструктуры (БД, RabbitMQ, Redis)..."
docker-compose up -d db rabbitmq redis

# Ожидание готовности БД
log_info "Ожидание готовности PostgreSQL..."
sleep 15

# Проверка здоровья БД
if docker-compose exec -T db pg_isready -U postgres > /dev/null 2>&1; then
    log_info "✓ PostgreSQL готов"
else
    log_error "✗ PostgreSQL не готов"
    exit 1
fi

# 5. Запуск orderservice
log_info "Запуск orderservice..."
docker-compose up -d orderservice

# Ожидание запуска
log_info "Ожидание запуска orderservice (миграция БД)..."
sleep 30

# Проверка логов orderservice
log_info "Проверка логов orderservice..."
docker-compose logs --tail=50 orderservice | grep -i "started\|error\|exception" || true

# Проверка здоровья orderservice
if curl -s http://localhost:8087/actuator/health > /dev/null 2>&1; then
    log_info "✓ OrderService запущен и отвечает"
else
    log_warning "⚠ OrderService может быть еще не готов, продолжаем..."
fi

# 6. Запуск client-storefront
log_info "Запуск client-storefront..."
docker-compose up -d client-storefront

# Ожидание запуска
log_info "Ожидание запуска client-storefront..."
sleep 20

# Проверка логов client-storefront
log_info "Проверка логов client-storefront..."
docker-compose logs --tail=30 client-storefront | grep -i "ready\|error\|failed" || true

# 7. Проверка API endpoints
echo ""
log_info "=========================================="
log_info "📋 Тестирование API endpoints"
log_info "=========================================="
echo ""

# Проверка Yandex Delivery Config
log_info "Тест 1: GET /api/yandex-delivery/config"
CONFIG_RESPONSE=$(curl -s http://localhost:8087/api/yandex-delivery/config || echo "FAILED")
if echo "$CONFIG_RESPONSE" | grep -q "sourcePlatformStation"; then
    log_info "✓ Yandex Delivery Config работает"
    echo "   Response: $CONFIG_RESPONSE"
else
    log_error "✗ Yandex Delivery Config не работает"
    echo "   Response: $CONFIG_RESPONSE"
fi
echo ""

# Проверка Delivery Methods
log_info "Тест 2: GET /api/delivery-methods"
DELIVERY_RESPONSE=$(curl -s http://localhost:8087/api/delivery-methods || echo "FAILED")
if echo "$DELIVERY_RESPONSE" | grep -q "Яндекс\|name"; then
    log_info "✓ Delivery Methods API работает"
    echo "   Найдено методов: $(echo "$DELIVERY_RESPONSE" | grep -o "name" | wc -l)"
else
    log_warning "⚠ Delivery Methods API не доступен или пуст"
fi
echo ""

# Проверка Payment Methods
log_info "Тест 3: GET /api/payment-methods"
PAYMENT_RESPONSE=$(curl -s http://localhost:8087/api/payment-methods || echo "FAILED")
if echo "$PAYMENT_RESPONSE" | grep -q "name"; then
    log_info "✓ Payment Methods API работает"
else
    log_warning "⚠ Payment Methods API не доступен"
fi
echo ""

# Проверка Frontend
log_info "Тест 4: GET http://localhost:3001 (frontend)"
FRONTEND_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3001 || echo "000")
if [ "$FRONTEND_RESPONSE" = "200" ]; then
    log_info "✓ Frontend доступен (HTTP $FRONTEND_RESPONSE)"
else
    log_warning "⚠ Frontend не доступен (HTTP $FRONTEND_RESPONSE)"
fi
echo ""

# 8. Проверка миграций БД
log_info "=========================================="
log_info "🗄️  Проверка миграций БД"
log_info "=========================================="
echo ""

log_info "Проверка таблицы orders..."
YANDEX_COLUMNS=$(docker-compose exec -T db psql -U postgres -d orderdb -c "\d orders" 2>/dev/null | grep yandex | wc -l || echo "0")

if [ "$YANDEX_COLUMNS" -gt 5 ]; then
    log_info "✓ Миграция Yandex Delivery выполнена ($YANDEX_COLUMNS полей)"
else
    log_error "✗ Поля Yandex Delivery не найдены в таблице orders"
fi

# Проверка метода доставки Яндекс
log_info "Проверка метода доставки 'Яндекс'..."
YANDEX_METHOD=$(docker-compose exec -T db psql -U postgres -d orderdb -c "SELECT name FROM delivery_methods WHERE name LIKE '%Яндекс%';" 2>/dev/null | grep Яндекс || echo "")

if [ -n "$YANDEX_METHOD" ]; then
    log_info "✓ Метод доставки Яндекс найден в БД"
    echo "   $YANDEX_METHOD"
else
    log_warning "⚠ Метод доставки Яндекс не найден в БД"
fi
echo ""

# 9. Итоговый статус
log_info "=========================================="
log_info "📊 Итоговый статус"
log_info "=========================================="
echo ""

docker-compose ps

echo ""
log_info "=========================================="
log_info "🎉 Сборка и тестирование завершены!"
log_info "=========================================="
echo ""
log_info "Доступные сервисы:"
log_info "  - OrderService API:      http://localhost:8087"
log_info "  - OrderService Health:   http://localhost:8087/actuator/health"
log_info "  - Yandex Delivery Config: http://localhost:8087/api/yandex-delivery/config"
log_info "  - Client Storefront:     http://localhost:3001"
log_info "  - PostgreSQL:            localhost:5432"
log_info "  - RabbitMQ Management:   http://localhost:15672 (admin/admin)"
echo ""
log_info "Команды для управления:"
log_info "  - Просмотр логов orderservice:       docker-compose logs -f orderservice"
log_info "  - Просмотр логов client-storefront:  docker-compose logs -f client-storefront"
log_info "  - Остановка всех сервисов:           docker-compose down"
log_info "  - Полная очистка:                    docker-compose down -v"
echo ""

# 10. Интерактивное меню
while true; do
    echo ""
    echo "Что делать дальше?"
    echo "  1) Просмотреть логи orderservice"
    echo "  2) Просмотреть логи client-storefront"
    echo "  3) Просмотреть все логи"
    echo "  4) Проверить статус контейнеров"
    echo "  5) Открыть frontend в браузере"
    echo "  6) Выход"
    echo ""
    read -p "Выберите опцию (1-6): " choice
    
    case $choice in
        1)
            docker-compose logs --tail=100 -f orderservice
            ;;
        2)
            docker-compose logs --tail=100 -f client-storefront
            ;;
        3)
            docker-compose logs --tail=50
            ;;
        4)
            docker-compose ps
            ;;
        5)
            if command -v open &> /dev/null; then
                open http://localhost:3001
            elif command -v xdg-open &> /dev/null; then
                xdg-open http://localhost:3001
            else
                log_info "Откройте браузер и перейдите на http://localhost:3001"
            fi
            ;;
        6)
            log_info "До свидания!"
            exit 0
            ;;
        *)
            log_error "Неверный выбор. Попробуйте снова."
            ;;
    esac
done
