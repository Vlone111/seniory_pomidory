# Order Service

A microservice for managing orders in the marketplace platform. Handles checkout process, order management, and integrates with Product Service and Cart Service via RabbitMQ.

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **PostgreSQL** with Spring Data JPA
- **RabbitMQ** for event-driven communication
- **Flyway** for database migrations
- **Lombok** for boilerplate reduction
- **MapStruct** for DTO mapping
- **Maven** for build management

## Features

### Core Functionality
- ✅ **Checkout Process** - Convert cart items to permanent orders
- ✅ **Order Management** - Create, view, and update orders
- ✅ **Delivery Methods** - Multiple delivery options with pricing
- ✅ **Payment Methods** - Various payment options
- ✅ **Order Status Tracking** - NEW, PAID, SHIPPED, COMPLETED, CANCELED
- ✅ **Event-Driven Architecture** - RabbitMQ integration for stock management and cart clearing

### Security
- ✅ **JWT Authentication** via KrakenD API Gateway
- ✅ **User Identification** via `X-User-Id` header
- ✅ **Authorization** - Users can only view their own orders
- ✅ **Admin Controls** - Status updates restricted to admins

### Data Integrity
- ✅ **Price Snapshot** - Stores price at time of purchase
- ✅ **Transactional Operations** - ACID compliance
- ✅ **Event Publishing** - Stock decrease and cart clearing events
- ✅ **Database Constraints** - Foreign keys and indexes

## Architecture

### Database Schema

#### Tables

**delivery_methods**
```sql
- id (UUID, PK)
- name (VARCHAR, UNIQUE)
- price (DECIMAL)
```

**payment_methods**
```sql
- id (UUID, PK)
- name (VARCHAR, UNIQUE)
- price (DECIMAL)
```

**orders**
```sql
- id (UUID, PK)
- user_id (UUID, INDEXED)
- recipient_id (UUID)
- delivery_method_id (UUID, FK)
- payment_method_id (UUID, FK)
- status (VARCHAR, INDEXED)
- total_amount (DECIMAL)
- created_at (TIMESTAMP, INDEXED)
```

**order_items**
```sql
- id (UUID, PK)
- order_id (UUID, FK, INDEXED)
- product_id (UUID, INDEXED)
- shop_id (UUID)
- quantity (INTEGER)
- price_per_item (DECIMAL)
```

### RabbitMQ Integration

**Exchange:** `order-exchange` (Topic)

**Published Events:**

1. **Stock Decrease Event** → `stock.decrease` queue
   ```json
   {
     "productId": "uuid",
     "sizeId": "uuid",
     "quantity": 5
   }
   ```

2. **Order Completed Event** → `order.completed` queue
   ```json
   {
     "userId": "uuid",
     "orderId": "uuid"
   }
   ```

## API Endpoints

All endpoints are accessible via KrakenD API Gateway at `http://localhost:8081`

### Public Endpoints

#### Get Delivery Methods
```http
GET /api/delivery-methods
```

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Courier",
    "price": 5.00
  }
]
```

#### Get Payment Methods
```http
GET /api/payment-methods
```

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Card",
    "price": 0.00
  }
]
```

### Authenticated Endpoints (User/Owner/Admin)

#### Create Order (Checkout)
```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipientId": "uuid",
  "deliveryMethodId": "uuid",
  "paymentMethodId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "shopId": "uuid",
      "quantity": 2,
      "pricePerItem": 29.99
    }
  ]
}
```

**Response:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "recipientId": "uuid",
  "deliveryMethod": {
    "id": "uuid",
    "name": "Courier",
    "price": 5.00
  },
  "paymentMethod": {
    "id": "uuid",
    "name": "Card",
    "price": 0.00
  },
  "status": "NEW",
  "totalAmount": 64.98,
  "createdAt": "2026-01-09T04:30:00",
  "items": [
    {
      "id": "uuid",
      "productId": "uuid",
      "shopId": "uuid",
      "quantity": 2,
      "pricePerItem": 29.99,
      "subtotal": 59.98
    }
  ]
}
```

#### Get User Orders
```http
GET /api/orders
Authorization: Bearer <token>
```

**Response:** Array of orders

#### Get Order by ID
```http
GET /api/orders/{orderId}
Authorization: Bearer <token>
```

**Response:** Order details with items

### Admin Endpoints

#### Update Order Status
```http
PATCH /api/orders/{orderId}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "PAID"
}
```

**Available Statuses:**
- `NEW` - Order created
- `PAID` - Payment confirmed
- `SHIPPED` - Order shipped
- `COMPLETED` - Order delivered
- `CANCELED` - Order canceled

## Business Logic

### Checkout Process

1. **Validate Input**
   - Check delivery and payment methods exist
   - Validate all order items

2. **Calculate Total**
   - Sum all item prices (quantity × price_per_item)
   - Add delivery method price
   - Add payment method price

3. **Create Order**
   - Save order with status `NEW`
   - Save all order items with current prices
   - Store product and shop references

4. **Publish Events**
   - Send stock decrease events for each item
   - Send order completed event to clear cart

5. **Return Response**
   - Return complete order details with items

### Order Authorization

- Users can only view their own orders
- Admins can view and update any order
- Order ownership verified via `X-User-Id` header

## Configuration

### Environment Variables

```yaml
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=orderdb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Server
SERVER_PORT=8087

# Logging
LOG_LEVEL=INFO
```

### Default Data

The service automatically creates default delivery and payment methods:

**Delivery Methods:**
- Courier - $5.00
- Self-pickup - $0.00
- Express Delivery - $10.00

**Payment Methods:**
- Card - $0.00
- Cash - $0.00
- Online Payment - $0.00

## Running the Service

### Local Development

```bash
# Build
mvn clean package

# Run
java -jar target/order-service-1.0.0.jar
```

### Docker

```bash
# Build image
docker build -t order-service .

# Run container
docker run -p 8087:8087 \
  -e DB_HOST=db \
  -e RABBITMQ_HOST=rabbitmq \
  order-service
```

### Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f orderservice

# Stop service
docker-compose stop orderservice
```

## Integration with Other Services

### Product Service
- **Consumes:** Stock decrease events
- **Purpose:** Reduce product inventory after order placement

### Cart Service
- **Consumes:** Order completed events
- **Purpose:** Clear user's cart after successful checkout

### Shop Service
- **Reference:** Stores shop_id in order items
- **Purpose:** Track which shop each product belongs to

### User Service
- **Reference:** Stores user_id and recipient_id
- **Purpose:** Link orders to users and delivery addresses

## Error Handling

### Custom Exceptions

- `OrderNotFoundException` - Order not found (404)
- `DeliveryMethodNotFoundException` - Delivery method not found (404)
- `PaymentMethodNotFoundException` - Payment method not found (404)
- `UnauthorizedAccessException` - User not authorized (403)

### Validation Errors

All request DTOs are validated:
- Required fields checked
- Minimum values enforced
- Data types validated

### Global Exception Handler

Provides consistent error responses:
```json
{
  "timestamp": "2026-01-09T04:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: uuid",
  "path": "/api/orders/uuid",
  "validationErrors": {}
}
```

## Testing

### Example: Create Order

```bash
# 1. Get delivery and payment methods
curl http://localhost:8081/api/delivery-methods
curl http://localhost:8081/api/payment-methods

# 2. Create order (with JWT token)
curl -X POST http://localhost:8081/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "user-uuid",
    "deliveryMethodId": "delivery-uuid",
    "paymentMethodId": "payment-uuid",
    "items": [
      {
        "productId": "product-uuid",
        "shopId": "shop-uuid",
        "quantity": 1,
        "pricePerItem": 99.99
      }
    ]
  }'

# 3. Get user orders
curl http://localhost:8081/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN"

# 4. Get specific order
curl http://localhost:8081/api/orders/{orderId} \
  -H "Authorization: Bearer YOUR_TOKEN"

# 5. Update order status (admin only)
curl -X PATCH http://localhost:8081/api/orders/{orderId}/status \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PAID"}'
```

## Monitoring

The service exposes standard Spring Boot Actuator endpoints (if configured):
- `/actuator/health` - Health check
- `/actuator/info` - Service information
- `/actuator/metrics` - Metrics

## Future Enhancements

- [ ] Order cancellation by users
- [ ] Order history with pagination
- [ ] Order search and filtering
- [ ] Email notifications on order status changes
- [ ] Invoice generation
- [ ] Refund processing
- [ ] Order tracking integration
- [ ] Multiple addresses per user
- [ ] Scheduled delivery times
- [ ] Order analytics and reporting

## License

Part of the Marketplace Platform
