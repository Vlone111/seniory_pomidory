#!/bin/bash

echo "🚀 Starting Microservices Architecture..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Error: docker-compose is not installed."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Start all services
echo "📦 Starting all services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
echo ""

# Wait for database
echo "Waiting for PostgreSQL..."
until docker exec db pg_isready -U postgres > /dev/null 2>&1; do
    sleep 2
done
echo "✅ PostgreSQL is ready"

# Wait for Keycloak
echo "Waiting for Keycloak (this may take 2-3 minutes)..."
until curl -s http://localhost:8080/health > /dev/null 2>&1; do
    sleep 5
done
echo "✅ Keycloak is ready"

# Wait for KrakenD
echo "Waiting for KrakenD..."
sleep 5
echo "✅ KrakenD is ready"

echo ""
echo "🎉 All services are up and running!"
echo ""
echo "📊 Service URLs:"
echo "  - API Gateway (KrakenD):    http://localhost:8081"
echo "  - Keycloak Admin:           http://localhost:8080 (admin/admin)"
echo "  - Grafana Dashboard:        http://localhost:3000 (admin/admin)"
echo "  - RabbitMQ Management:      http://localhost:15672 (admin/admin)"
echo "  - MinIO Console:            http://localhost:9001 (admin/admin1234)"
echo "  - Prometheus:               http://localhost:9090"
echo ""
echo "📝 Check logs with: docker-compose logs -f [service-name]"
echo "🛑 Stop all with: docker-compose down"
echo ""
echo "📖 See START.md for detailed documentation"
