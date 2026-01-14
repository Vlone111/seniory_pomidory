# 🚀 Quick Start Guide

This guide will help you start the entire microservices architecture with a single command.

## 📋 Prerequisites

- Docker and Docker Compose installed
- At least 8GB of RAM available
- Ports available: 3000, 3100, 3200, 4317, 4318, 5432, 5672, 6379, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 9000, 9001, 9080, 9090, 15672, 15692

## 🎯 Quick Start

### Start Everything
```bash
docker-compose up -d
```

### Check Status
```bash
docker-compose ps
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f shop
docker-compose logs -f krakend
docker-compose logs -f keycloak
```

### Stop Everything
```bash
docker-compose down
```

### Clean Restart (Remove all data)
```bash
docker-compose down -v
docker-compose up -d
```

## 🏗️ Architecture Overview

### Services & Ports

| Service | Internal Port | External Port | Description |
|---------|--------------|---------------|-------------|
| **KrakenD** | 8080 | 8081 | API Gateway |
| **Keycloak** | 8080 | 8080 | Authentication & Authorization |
| **Shop Service** | 8080 | 8080 | Shop management |
| **User Service** | 8080 | 8080 | User management |
| **Product Service** | 8083 | 8083 | Product catalog |
| **Cart Service** | 8084 | 8084 | Shopping cart |
| **News Service** | 8085 | 8085 | News articles |
| **File Service** | 8082 | 8082 | File upload/download |
| **Basket Service** | 8080 | 8086 | Basket management |
| **PostgreSQL** | 5432 | 5432 | Database |
| **MinIO** | 9000/9001 | 9000/9001 | Object storage |
| **Redis** | 6379 | 6379 | Cache |
| **RabbitMQ** | 5672/15672 | 5672/15672 | Message broker |
| **Grafana** | 3000 | 3000 | Monitoring dashboard |
| **Prometheus** | 9090 | 9090 | Metrics |
| **Loki** | 3100 | 3100 | Logs |
| **Tempo** | 3200 | 3200 | Traces |

### Databases Created

The PostgreSQL container automatically creates these databases:
- `basket_db` - Basket service
- `shop_db` - Shop service
- `keycloak_postgres` - Keycloak
- `userservice_db` - User service
- `productdb` - Product service
- `cartdb` - Cart service
- `newsdb` - News service

## 🔐 Access Information

### Keycloak Admin Console
- URL: http://localhost:8080
- Username: `admin`
- Password: `admin`
- Realm: `main_one`

### Grafana Dashboard
- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

### RabbitMQ Management
- URL: http://localhost:15672
- Username: `admin`
- Password: `admin`

### MinIO Console
- URL: http://localhost:9001
- Username: `admin`
- Password: `admin1234`

### API Gateway (KrakenD)
- URL: http://localhost:8081
- All API requests should go through this gateway

## 📡 API Endpoints (via KrakenD)

All requests go through KrakenD at `http://localhost:8081`

### Public Endpoints (No Auth Required)
- `POST /register` - Register user
- `POST /auth/registerowner` - Register owner
- `POST /auth/login` - Login
- `GET /api/shops` - List all shops
- `GET /api/shops/{shopId}` - Get shop by ID
- `GET /api/shops/url/{shopUrl}` - Get shop by URL
- `GET /api/shops/owner/{ownerId}` - Get shops by owner
- `GET /api/products` - List products
- `GET /api/products/{id}` - Get product
- `GET /api/brands` - List brands
- `GET /api/categories` - List categories
- `GET /api/news` - List news articles
- `GET /api/news/{slug}` - Get news article
- `GET /api/files/download/{fileName}` - Download file

### Authenticated Endpoints

**Shop Management (Owner role)**
- `POST /api/shops` - Create shop
- `PUT /api/shops/{shopId}` - Update shop
- `DELETE /api/shops/{shopId}` - Delete shop
- `GET /api/shops/my-shops` - Get my shops

**Product Management (Owner role)**
- `POST /api/products` - Create product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

**Admin Endpoints (Admin role)**
- `POST /api/brands` - Create brand
- `POST /api/categories` - Create category
- `POST /api/news` - Create news article
- `PUT /api/news/{id}` - Update news article
- `DELETE /api/news/{id}` - Delete news article
- `POST /auth/assign-role` - Assign roles

**Cart Operations (User/Owner/Admin roles)**
- `GET /api/cart` - Get cart
- `POST /api/cart/add` - Add to cart
- `PATCH /api/cart/items/{itemId}` - Update cart item
- `DELETE /api/cart/items/{itemId}` - Remove from cart
- `DELETE /api/cart` - Clear cart

**File Operations (Owner/Admin roles)**
- `POST /api/files/upload` - Upload file
- `DELETE /api/files/{fileName}` - Delete file

## 🔧 Troubleshooting

### Services Not Starting
```bash
# Check logs for specific service
docker-compose logs <service-name>

# Common issues:
# 1. Port already in use - stop conflicting services
# 2. Not enough memory - increase Docker memory limit
# 3. Database not ready - wait for healthcheck to pass
```

### Database Connection Issues
```bash
# Check if database is healthy
docker-compose ps db

# Connect to database
docker exec -it db psql -U postgres

# List databases
\l

# Connect to specific database
\c shop_db
```

### Reset Everything
```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Remove all images (optional)
docker-compose down --rmi all

# Start fresh
docker-compose up -d
```

### Check Service Health
```bash
# Shop Service
curl http://localhost:8080/actuator/health

# File Service
curl http://localhost:8082/actuator/health

# Product Service
curl http://localhost:8083/actuator/health

# Cart Service
curl http://localhost:8084/actuator/health

# News Service
curl http://localhost:8085/actuator/health
```

## 📊 Monitoring

### View Metrics
1. Open Grafana: http://localhost:3000
2. Login with admin/admin
3. Navigate to Dashboards
4. View pre-configured dashboards for each service

### View Logs
1. Open Grafana: http://localhost:3000
2. Go to Explore
3. Select Loki as data source
4. Query logs by service: `{container_name="shop"}`

### View Traces
1. Open Grafana: http://localhost:3000
2. Go to Explore
3. Select Tempo as data source
4. Search for traces

## 🔄 Development Workflow

### Rebuild Single Service
```bash
# Rebuild and restart specific service
docker-compose up -d --build shop

# View logs
docker-compose logs -f shop
```

### Update Configuration
```bash
# After changing application.yml or Dockerfile
docker-compose up -d --build <service-name>

# After changing docker-compose.yml
docker-compose up -d
```

## 🎯 Testing the Setup

### 1. Register a User
```bash
curl -X POST http://localhost:8081/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=marketplace-client&username=testuser&password=password123"
```

### 3. Create a Shop (with token from login)
```bash
curl -X POST http://localhost:8081/api/shops \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "shopName": "My Shop",
    "shopUrl": "my-shop",
    "description": "Test shop"
  }'
```

## 📝 Notes

- First startup may take 5-10 minutes as all services build and initialize
- Keycloak takes ~2 minutes to fully start
- Database migrations run automatically on first startup
- MinIO bucket is created automatically
- All services have health checks and will restart if they fail

## 🆘 Support

If you encounter issues:
1. Check the logs: `docker-compose logs -f`
2. Verify all services are running: `docker-compose ps`
3. Check database connectivity: `docker exec -it db psql -U postgres -c '\l'`
4. Ensure all required ports are available
5. Try a clean restart: `docker-compose down -v && docker-compose up -d`
