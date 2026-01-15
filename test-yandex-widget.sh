#!/bin/bash

# Скрипт для тестирования интеграции виджета Яндекс.Доставки
# Автор: AI Assistant
# Дата: 2026-01-15

set -e

echo "🧪 Тестирование интеграции виджета Яндекс.Доставки"
echo "=================================================="
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Функция для проверки
check_service() {
    local service_name=$1
    local service_url=$2
    local description=$3
    
    echo -n "Проверка $description... "
    
    if curl -s -f -o /dev/null "$service_url"; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        return 1
    fi
}

# Счетчики
total_tests=0
passed_tests=0

echo "1️⃣  Проверка доступности сервисов"
echo "-----------------------------------"

# Проверка orderservice
total_tests=$((total_tests + 1))
if check_service "orderservice" "http://localhost:8087/actuator/health" "OrderService"; then
    passed_tests=$((passed_tests + 1))
fi

# Проверка client-storefront
total_tests=$((total_tests + 1))
if check_service "client-storefront" "http://localhost:3001" "Client Storefront"; then
    passed_tests=$((passed_tests + 1))
fi

echo ""
echo "2️⃣  Проверка API Яндекс.Доставки"
echo "---------------------------------"

# Проверка конфигурации
total_tests=$((total_tests + 1))
echo -n "Проверка /api/yandex-delivery/config... "
config_response=$(curl -s http://localhost:8087/api/yandex-delivery/config)

if echo "$config_response" | grep -q "sourcePlatformStation"; then
    echo -e "${GREEN}✓ OK${NC}"
    passed_tests=$((passed_tests + 1))
    echo "   Конфигурация:"
    echo "$config_response" | python3 -m json.tool 2>/dev/null || echo "$config_response"
else
    echo -e "${RED}✗ FAIL${NC}"
    echo "   Ответ: $config_response"
fi

echo ""
echo "3️⃣  Проверка файлов интеграции"
echo "-------------------------------"

# Проверка компонента виджета
total_tests=$((total_tests + 1))
if [ -f "client-storefront/src/components/delivery/YandexDeliveryWidget.tsx" ]; then
    echo -e "${GREEN}✓${NC} YandexDeliveryWidget.tsx существует"
    passed_tests=$((passed_tests + 1))
else
    echo -e "${RED}✗${NC} YandexDeliveryWidget.tsx не найден"
fi

# Проверка сервиса
total_tests=$((total_tests + 1))
if [ -f "client-storefront/src/services/yandex-delivery.service.ts" ]; then
    echo -e "${GREEN}✓${NC} yandex-delivery.service.ts существует"
    passed_tests=$((passed_tests + 1))
else
    echo -e "${RED}✗${NC} yandex-delivery.service.ts не найден"
fi

# Проверка контроллера
total_tests=$((total_tests + 1))
if [ -f "orderservice/src/main/java/com/marketplace/orderservice/controller/YandexDeliveryProxyController.java" ]; then
    echo -e "${GREEN}✓${NC} YandexDeliveryProxyController.java существует"
    passed_tests=$((passed_tests + 1))
else
    echo -e "${RED}✗${NC} YandexDeliveryProxyController.java не найден"
fi

# Проверка конфигурации
total_tests=$((total_tests + 1))
if [ -f "orderservice/src/main/java/com/marketplace/orderservice/config/YandexDeliveryProperties.java" ]; then
    echo -e "${GREEN}✓${NC} YandexDeliveryProperties.java существует"
    passed_tests=$((passed_tests + 1))
else
    echo -e "${RED}✗${NC} YandexDeliveryProperties.java не найден"
fi

echo ""
echo "4️⃣  Проверка документации"
echo "-------------------------"

# Проверка документов
docs=("YANDEX_DELIVERY_WIDGET_SETUP.md" "YANDEX_WIDGET_QUICK_START.md" "YANDEX_DELIVERY_CHANGES_SUMMARY.md")

for doc in "${docs[@]}"; do
    total_tests=$((total_tests + 1))
    if [ -f "$doc" ]; then
        echo -e "${GREEN}✓${NC} $doc существует"
        passed_tests=$((passed_tests + 1))
    else
        echo -e "${RED}✗${NC} $doc не найден"
    fi
done

echo ""
echo "=================================================="
echo "📊 Результаты тестирования"
echo "=================================================="
echo ""
echo "Всего тестов: $total_tests"
echo -e "Пройдено: ${GREEN}$passed_tests${NC}"
echo -e "Провалено: ${RED}$((total_tests - passed_tests))${NC}"

if [ $passed_tests -eq $total_tests ]; then
    echo ""
    echo -e "${GREEN}✅ Все тесты пройдены успешно!${NC}"
    echo ""
    echo "🚀 Готово к использованию!"
    echo ""
    echo "Следующие шаги:"
    echo "1. Откройте http://localhost:3001/checkout"
    echo "2. Заполните форму оформления заказа"
    echo "3. Выберите способ доставки"
    echo "4. Используйте виджет Яндекс.Доставки"
    echo ""
    exit 0
else
    echo ""
    echo -e "${RED}❌ Некоторые тесты не пройдены${NC}"
    echo ""
    echo "Возможные причины:"
    echo "1. Сервисы не запущены (запустите: docker-compose up)"
    echo "2. Порты заняты другими приложениями"
    echo "3. Файлы не были созданы корректно"
    echo ""
    echo "Для получения помощи см. YANDEX_WIDGET_QUICK_START.md"
    echo ""
    exit 1
fi
