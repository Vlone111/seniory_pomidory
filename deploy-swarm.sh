#!/bin/bash

set -e

echo "🚀 Развертывание проекта на Docker Swarm"
echo "======================================"

# Проверка Docker Swarm статуса
if ! docker info | grep -q "Swarm: active"; then
    echo "❌ Docker Swarm не активен. Инициализация..."
    docker swarm init --advertise-addr $(hostname -I | awk '{print $1}')
    echo "✅ Docker Swarm инициализирован"
else
    echo "✅ Docker Swarm уже активен"
fi

# Проверка узлов
echo ""
echo "📊 Статус узлов Swarm:"
docker node ls

# Создание сетей (если не существуют)
echo ""
echo "🌐 Создание overlay сетей..."

networks=("keycloak_net" "microservices" "observability" "files" "fashion-network")

for network in "${networks[@]}"; do
    if ! docker network ls | grep -q "$network"; then
        echo "  Создание сети: $network"
        docker network create --driver overlay --attachable "$network"
    else
        echo "  Сеть $network уже существует"
    fi
done

echo "✅ Сети созданы"

# Создание secrets для безопасности
echo ""
echo "🔐 Создание secrets..."

# Создание secrets (если не существуют)
secrets=(
    "postgres_password:postgres"
    "keycloak_admin_password:admin"
    "rabbitmq_password:admin"
    "minio_password:admin1234"
)

for secret in "${secrets[@]}"; do
    name=$(echo $secret | cut -d: -f1)
    value=$(echo $secret | cut -d: -f2)
    
    if ! docker secret ls | grep -q "$name"; then
        echo "  Создание secret: $name"
        echo "$value" | docker secret create "$name" -
    else
        echo "  Secret $name уже существует"
    fi
done

echo "✅ Secrets созданы"

# Очистка предыдущего стека (если существует)
echo ""
echo "🧹 Очистка предыдущего стека..."
if docker stack ls | grep -q "marketplace"; then
    echo "  Удаление существующего стека marketplace..."
    docker stack rm marketplace
    echo "  Ожидание удаления стека..."
    sleep 10
fi

# Развертывание стека
echo ""
echo "📦 Развертывание стека marketplace..."
docker stack deploy -c docker-stack.yml marketplace

echo "✅ Стек развернут"

# Ожидание запуска сервисов
echo ""
echo "⏳ Ожидание запуска сервисов..."
sleep 30

# Проверка статуса сервисов
echo ""
echo "📊 Статус сервисов:"
docker stack services marketplace

# Проверка здоровья критичных сервисов
echo ""
echo "🏥 Проверка здоровья сервисов..."

services=("db" "redis" "keycloak" "rabbitmq")

for service in "${services[@]}"; do
    echo "  Проверка $service..."
    for i in {1..10}; do
        if docker service ps marketplace_$service --format "{{.CurrentState}}" | grep -q "Running"; then
            echo "    ✅ $service работает"
            break
        else
            echo "    ⏳ Ожидание $service... ($i/10)"
            sleep 10
        fi
    done
done

# Вывод информации для доступа
echo ""
echo "🎉 Развертывание завершено!"
echo "=========================="
echo ""
echo "📡 Доступные сервисы:"
echo "  🌐 Frontend:          http://localhost:3000"
echo "  🔐 Keycloak:          http://localhost:8080 (admin/admin)"
echo "  🚪 API Gateway:       http://localhost:8081"
echo "  📊 Grafana:           http://localhost:3400 (admin/admin)"
echo "  📈 Prometheus:        http://localhost:9090"
echo "  🗄️  PostgreSQL:        localhost:5432"
echo "  📬 RabbitMQ:          http://localhost:15672 (admin/admin)"
echo "  📁 MinIO:             http://localhost:9001 (admin/admin1234)"
echo ""
echo "🔍 Полезные команды:"
echo "  docker stack services marketplace      # Показать сервисы"
echo "  docker stack ps marketplace            # Показать задачи"
echo "  docker service logs marketplace_<service>  # Логи сервиса"
echo "  docker service scale marketplace_<service>=<replicas>  # Масштабирование"
echo ""
echo "⚠️  Первоначальный запуск может занять 5-10 минут"

# Мониторинг
echo ""
echo "📊 Мониторинг запуска (Ctrl+C для выхода):"
while true; do
    echo "$(date): Проверка статуса..."
    running=$(docker stack services marketplace --format "{{.Name}}: {{.Replicas}}" | grep -c "1/1\|2/2")
    total=$(docker stack services marketplace --format "{{.Name}}" | wc -l)
    echo "Запущено сервисов: $running/$total"
    sleep 30
done
