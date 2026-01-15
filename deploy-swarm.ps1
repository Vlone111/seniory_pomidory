# Deploy-Swarm.ps1
# Скрипт для развертывания проекта на Docker Swarm в Windows

Write-Host "🚀 Развертывание проекта на Docker Swarm" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Yellow

# Проверка Docker Swarm статуса
$swarmStatus = docker info | Select-String "Swarm: active"
if (-not $swarmStatus) {
    Write-Host "❌ Docker Swarm не активен. Инициализация..." -ForegroundColor Red
    $ip = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Ethernet*").IPAddress[0]
    docker swarm init --advertise-addr $ip
    Write-Host "✅ Docker Swarm инициализирован" -ForegroundColor Green
} else {
    Write-Host "✅ Docker Swarm уже активен" -ForegroundColor Green
}

# Проверка узлов
Write-Host ""
Write-Host "📊 Статус узлов Swarm:" -ForegroundColor Cyan
docker node ls

# Создание сетей
Write-Host ""
Write-Host "🌐 Создание overlay сетей..." -ForegroundColor Cyan

$networks = @("keycloak_net", "microservices", "observability", "files", "fashion-network")

foreach ($network in $networks) {
    $existingNetwork = docker network ls | Select-String $network
    if (-not $existingNetwork) {
        Write-Host "  Создание сети: $network" -ForegroundColor Yellow
        docker network create --driver overlay --attachable $network
    } else {
        Write-Host "  Сеть $network уже существует" -ForegroundColor Green
    }
}

Write-Host "✅ Сети созданы" -ForegroundColor Green

# Создание secrets
Write-Host ""
Write-Host "🔐 Создание secrets..." -ForegroundColor Cyan

$secrets = @{
    "postgres_password" = "postgres"
    "keycloak_admin_password" = "admin"
    "rabbitmq_password" = "admin"
    "minio_password" = "admin1234"
}

foreach ($secret in $secrets.GetEnumerator()) {
    $existingSecret = docker secret ls | Select-String $secret.Name
    if (-not $existingSecret) {
        Write-Host "  Создание secret: $($secret.Name)" -ForegroundColor Yellow
        $secret.Value | docker secret create $secret.Name -
    } else {
        Write-Host "  Secret $($secret.Name) уже существует" -ForegroundColor Green
    }
}

Write-Host "✅ Secrets созданы" -ForegroundColor Green

# Очистка предыдущего стека
Write-Host ""
Write-Host "🧹 Очистка предыдущего стека..." -ForegroundColor Cyan

$existingStack = docker stack ls | Select-String "marketplace"
if ($existingStack) {
    Write-Host "  Удаление существующего стека marketplace..." -ForegroundColor Yellow
    docker stack rm marketplace
    Write-Host "  Ожидание удаления стека..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
}

# Развертывание стека
Write-Host ""
Write-Host "📦 Развертывание стека marketplace..." -ForegroundColor Cyan
docker stack deploy -c docker-stack.yml marketplace

Write-Host "✅ Стек развернут" -ForegroundColor Green

# Ожидание запуска сервисов
Write-Host ""
Write-Host "⏳ Ожидание запуска сервисов..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Проверка статуса сервисов
Write-Host ""
Write-Host "📊 Статус сервисов:" -ForegroundColor Cyan
docker stack services marketplace

# Проверка здоровья критичных сервисов
Write-Host ""
Write-Host "🏥 Проверка здоровья сервисов..." -ForegroundColor Cyan

$services = @("db", "redis", "keycloak", "rabbitmq")

foreach ($service in $services) {
    Write-Host "  Проверка $service..." -ForegroundColor Yellow
    for ($i = 1; $i -le 10; $i++) {
        $serviceStatus = docker service ps marketplace_$service --format "{{.CurrentState}}" | Select-String "Running"
        if ($serviceStatus) {
            Write-Host "    ✅ $service работает" -ForegroundColor Green
            break
        } else {
            Write-Host "    ⏳ Ожидание $service... ($i/10)" -ForegroundColor Yellow
            Start-Sleep -Seconds 10
        }
    }
}

# Вывод информации для доступа
Write-Host ""
Write-Host "🎉 Развертывание завершено!" -ForegroundColor Green
Write-Host "==========================" -ForegroundColor Yellow
Write-Host ""
Write-Host "📡 Доступные сервисы:" -ForegroundColor Cyan
Write-Host "  🌐 Frontend:          http://localhost:3000"
Write-Host "  🔐 Keycloak:          http://localhost:8080 (admin/admin)"
Write-Host "  🚪 API Gateway:       http://localhost:8081"
Write-Host "  📊 Grafana:           http://localhost:3400 (admin/admin)"
Write-Host "  📈 Prometheus:        http://localhost:9090"
Write-Host "  🗄️  PostgreSQL:        localhost:5432"
Write-Host "  📬 RabbitMQ:          http://localhost:15672 (admin/admin)"
Write-Host "  📁 MinIO:             http://localhost:9001 (admin/admin1234)"
Write-Host ""
Write-Host "🔍 Полезные команды:" -ForegroundColor Cyan
Write-Host "  docker stack services marketplace      # Показать сервисы"
Write-Host "  docker stack ps marketplace            # Показать задачи"
Write-Host "  docker service logs marketplace_<service>  # Логи сервиса"
Write-Host "  docker service scale marketplace_<service>=<replicas>  # Масштабирование"
Write-Host ""
Write-Host "⚠️  Первоначальный запуск может занять 5-10 минут" -ForegroundColor Yellow

# Мониторинг
Write-Host ""
Write-Host "📊 Мониторинг запуска (Ctrl+C для выхода):" -ForegroundColor Cyan

try {
    while ($true) {
        Write-Host "$(Get-Date): Проверка статуса..."
        $services = docker stack services marketplace --format "{{.Name}}: {{.Replicas}}"
        $running = ($services | Select-String "1/1|2/2").Count
        $total = $services.Count
        Write-Host "Запущено сервисов: $running/$total"
        Start-Sleep -Seconds 30
    }
} catch {
    Write-Host "Мониторинг остановлен" -ForegroundColor Yellow
}
