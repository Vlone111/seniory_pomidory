# Cart Service

Cart Service for the Marketplace Microservices system.

## Tech Stack

- **Language:** Java 17+
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA (Hibernate)
- **Messaging:** RabbitMQ
- **Tools:** Lombok, MapStruct, Flyway

## Features

- **Cart Management:** Temporary storage of items a user intends to buy
- **Single Active Basket:** Each user has exactly ONE active basket
- **Shop-Aware:** Stores items from different shops with shop_id tracking
- **Auto-Clear on Order:** Listens to RabbitMQ `order.completed` events to clear cart
- **X-User-Id Security:** Always uses header-based user identification

## Business Context

The Cart Service manages temporary storage of items a user intends to buy. It sits behind **KrakenD API Gateway**, which provides the `X-User-Id` header (UUID) for every request. Each user has exactly ONE active basket, and the service is "Shop-aware" (it stores items from different shops).

## Database Schema

### Entities

1. **Basket**
   - `id` (UUID, PK)
   - `user_id` (UUID, Unique, Indexed) - Matches X-User-Id from Keycloak
   - `updated_at` (Timestamp)

2. **BasketItem**
   - `id` (UUID, PK)
   - `basket` (ManyToOne -> Basket)
   - `product_id` (UUID, Not Null) - Reference to Product Service
   - `shop_id` (UUID, Not Null) - Reference to the Shop that owns the product
   - `quantity` (Integer, min = 1)
   - `price_at_add` (BigDecimal) - Price when the item was added

## API Endpoints

### Cart Endpoints

- `GET /api/cart` - Get current user's basket
  - **Headers:** `X-User-Id` (UUID)
  - **Response:** Cart with items and total price

- `POST /api/cart/add` - Add a product to the basket
  - **Headers:** `X-User-Id` (UUID)
  - **Request Body:**
    ```json
    {
      "productId": "uuid",
      "shopId": "uuid",
      "quantity": 1,
      "price": 99.99
    }
    ```
  - **Logic:** If item exists, increases quantity. If not, creates new BasketItem.

- `PATCH /api/cart/items/{itemId}` - Update item quantity
  - **Headers:** `X-User-Id` (UUID)
  - **Request Body:**
    ```json
    {
      "quantity": 3
    }
    ```

- `DELETE /api/cart/items/{itemId}` - Remove item from cart
  - **Headers:** `X-User-Id` (UUID)

- `DELETE /api/cart` - Clear the entire basket
  - **Headers:** `X-User-Id` (UUID)

## RabbitMQ Integration

### Order Completed Queue
- **Queue Name:** `order.completed`
- **Payload:**
  ```json
  {
    "userId": "uuid"
  }
  ```
- **Behavior:** When an order is successfully placed in the Order Service, the Cart Service clears the basket for that specific user_id.

## Security

- Always uses the `X-User-Id` header to identify which basket to work with
- Never trusts a `userId` passed in the JSON body
- KrakenD API Gateway handles authentication and JWT verification

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cartdb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Server
SERVER_PORT=8084

# Logging
LOG_LEVEL=INFO
SHOW_SQL=false
```

## Running the Application

### Prerequisites
- Java 17+
- PostgreSQL
- RabbitMQ
- Maven

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Or with custom configuration:
```bash
java -jar target/cart-service-1.0.0.jar
```

## Database Migration

Flyway is used for database migrations. Migrations are located in:
```
src/main/resources/db/migration/
```

Migrations run automatically on application startup.

## Key Features

✅ **One basket per user** - Unique constraint on `user_id`  
✅ **X-User-Id header** extraction in all endpoints  
✅ **Shop-aware** - Tracks which shop each product belongs to  
✅ **Price snapshot** - Stores `price_at_add` for historical accuracy  
✅ **Auto-increment quantity** - If same product added, quantity increases  
✅ **RabbitMQ listener** - Auto-clears cart on order completion  
✅ **Cascade delete** - Removing basket removes all items  
✅ **Total calculation** - Automatic subtotal and total price calculation  

## Development Notes

- All primary keys use UUID
- `user_id` is indexed and unique for fast lookups
- JPA Auditing is enabled for `updated_at` field
- MapStruct handles DTO-Entity mapping with custom calculations
- Global exception handler provides consistent error responses
- Basket items are fetched eagerly when needed using JOIN FETCH
