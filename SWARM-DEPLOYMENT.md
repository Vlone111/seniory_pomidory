# 🚀 Развертывание на Docker Swarm

Это руководство поможет вам развернуть микросервисную архитектуру на Docker Swarm.

## 📋 Предварительные требования

- Docker Desktop с включенным Swarm режимом
- Минимум 8GB RAM
- Доступные порты: 3000, 3100, 3200, 4317, 4318, 5432, 5672, 6379, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 9000, 9001, 9080, 9090, 15672, 15692

## 🎯 Быстрый старт

### Вариант 1: Автоматическое развертывание (рекомендуется)

#### Windows PowerShell:
```powershell
.\deploy-swarm.ps1
```

#### Linux/Mac:
```bash
./deploy-swarm.sh
```

### Вариант 2: Ручное развертывание

#### 1. Инициализация Swarm
```bash
docker swarm init --advertise-addr <IP-АДРЕС>
```

#### 2. Создание сетей
```bash
docker network create --driver overlay --attachable keycloak_net
docker network create --driver overlay --attachable microservices
docker network create --driver overlay --attachable observability
docker network create --driver overlay --attachable files
docker network create --driver overlay --attachable fashion-network
```

#### 3. Создание Secrets
```bash
echo "postgres" | docker secret create postgres_password -
echo "admin" | docker secret create keycloak_admin_password -
echo "admin" | docker secret create rabbitmq_password -
echo "admin1234" | docker secret create minio_password -
```

#### 4. Развертывание стека
```bash
docker stack deploy -c docker-stack.yml marketplace
```

## 📊 Управление стеком

### Основные команды
```bash
# Показать все стеки
docker stack ls

# Показать сервисы в стеке
docker stack services marketplace

# Показать задачи (контейнеры) в стеке
docker stack ps marketplace

# Показать логи сервиса
docker service logs marketplace_productservice

# Масштабировать сервис
docker service scale marketplace_productservice=3

# Обновить сервис
docker service update marketplace_productservice

# Откатить сервис
docker service rollback marketplace_productservice

# Удалить стек
docker stack rm marketplace
```

### Мониторинг
```bash
# Показать узлы Swarm
docker node ls

# Показать ресурсы узлов
docker node ps

# Показать детальную информацию о сервисе
docker service inspect marketplace_productservice
```

## 🔧 Конфигурация

### Репликация сервисов

В `docker-stack.yml` настроены следующие репликации:

| Сервис | Реплики | Причина |
|--------|---------|---------|
| Redis | 1 | Stateful сервис |
| PostgreSQL | 1 | Stateful сервис |
| Keycloak | 1 | Stateful сервис |
| RabbitMQ | 1 | Stateful сервис |
| MinIO | 1 | Stateful сервис |
| Микросервисы | 2 | High availability |
| Frontend | 2 | Load balancing |
| Monitoring | 1 | Centralized |

### Health Checks

Все сервисы имеют health checks для автоматического перезапуска:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```

### Размещение сервисов

Сервисы размещаются только на worker узлах:

```yaml
deploy:
  placement:
    constraints:
      - node.role == worker
```

## 🌐 Доступ к сервисам

| Сервис | URL | Логин/Пароль |
|--------|-----|-------------|
| Frontend | http://localhost:3000 | - |
| Keycloak Admin | http://localhost:8080 | admin/admin |
| API Gateway | http://localhost:8081 | - |
| Grafana | http://localhost:3400 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| RabbitMQ Management | http://localhost:15672 | admin/admin |
| MinIO Console | http://localhost:9001 | admin/admin1234 |

## 🔄 Обновление сервисов

### Rolling Update
```bash
# Обновить образ сервиса
docker service update --image new-image marketplace_productservice

# Обновить с параметрами
docker service update \
  --update-parallelism 1 \
  --update-delay 10s \
  --update-failure-action rollback \
  marketplace_productservice
```

### Blue-Green Deployment
```bash
# Развернуть новую версию
docker stack deploy -c docker-stack-v2.yml marketplace-v2

# Переключить трафик
# (настроить в load balancer)
```

## 📈 Масштабирование

### Горизонтальное масштабирование
```bash
# Масштабировать все микросервисы до 3 реплик
docker service scale marketplace_productservice=3
docker service scale marketplace_cartservice=3
docker service scale marketplace_newsservice=3
docker service scale marketplace_orderservice=3
docker service scale marketplace_userservice=3
docker service scale marketplace_shop=3
docker service scale marketplace_fileservice=3
```

### Вертикальное масштабирование
```bash
# Изменить ресурсы сервиса
docker service update \
  --limit-cpu 2.0 \
  --limit-memory 2G \
  --reserve-cpu 1.0 \
  --reserve-memory 1G \
  marketplace_productservice
```

## 🔒 Безопасность

### Secrets Management
```bash
# Показать все secrets
docker secret ls

# Показать детали secret
docker secret inspect postgres_password

# Создать новый secret
echo "new-password" | docker secret create new_secret -

# Удалить secret
docker secret rm old_secret
```

### Network Security
```bash
# Показать сети
docker network ls

# Показать детали сети
docker network inspect microservices

# Отключить сервис от сети
docker service update --network-rm microservices marketplace_productservice
```

## 🐛 Troubleshooting

### Проверка статуса
```bash
# Показать сервисы с проблемами
docker stack services marketplace --filter "desired-replicas>running-replicas"

# Показать логи проблемного сервиса
docker service logs --follow marketplace_productservice

# Показать детали контейнера
docker inspect $(docker ps -q --filter "name=marketplace_productservice")
```

### Перезапуск сервисов
```bash
# Перезапустить конкретный сервис
docker service update --force marketplace_productservice

# Перезапустить весь стек
docker stack rm marketplace
docker stack deploy -c docker-stack.yml marketplace
```

### Очистка
```bash
# Очистка неиспользуемых ресурсов
docker system prune -f

# Очистка включая volumes
docker system prune -a --volumes -f

# Очистка Swarm ресурсов
docker swarm leave --force
```

## 📊 Мониторинг

### Grafana Dashboards
- System Overview: общая система
- Service Metrics: метрики сервисов
- Database Performance: производительность БД
- Message Queue: RabbitMQ метрики

### Prometheus Queries
```promql
# Запущенные сервисы
up{job="docker-swarm"}

# CPU использование
rate(container_cpu_usage_seconds_total[5m])

# Memory использование
container_memory_usage_bytes

# Network трафик
rate(container_network_transmit_bytes_total[5m])
```

## 🚀 Production рекомендации

### High Availability
1. Используйте минимум 3 manager узла
2. Размещайте сервисы на разных узлах
3. Настройте backup для баз данных
4. Используйте external load balancer

### Performance
1. Настройте resource limits
2. Используйте SSD для volumes
3. Оптимизируйте образы Docker
4. Настройте connection pooling

### Backup Strategy
```bash
# Backup PostgreSQL
docker exec marketplace_db.1.$(docker service ps -q marketplace_db | head -1) \
  pg_dump -U postgres > backup.sql

# Backup volumes
docker run --rm -v postgres_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres_backup.tar.gz -C /data .
```

## 📞 Поддержка

Если возникли проблемы:
1. Проверьте логи: `docker service logs marketplace_<service>`
2. Проверьте статус: `docker stack ps marketplace`
3. Проверьте ресурсы: `docker node ls`
4. Попробуйте перезапустить: `docker service update --force marketplace_<service>`
