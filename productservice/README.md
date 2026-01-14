# Product Service

Product Service for the Marketplace Microservices system.

## Tech Stack

- **Language:** Java 17+
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA (Hibernate)
- **Messaging:** RabbitMQ
- **Tools:** Lombok, MapStruct, Flyway

## Features

- **Product Management:** CRUD operations for products with multi-tenancy support (shop-based)
- **Brand Management:** Manage product brands
- **Category Management:** Hierarchical category structure
- **Size Management:** Product size variants
- **Inventory Management:** Stock tracking per product-size combination
- **Stock Decrease Listener:** RabbitMQ consumer for stock updates
- **Global Search:** Filter products by category, brand, price range, and text search

## Architecture

### Authentication & Authorization
- Authentication is handled by **KrakenD API Gateway**
- The Gateway verifies JWT and Roles
- User ID is passed via HTTP Header `X-User-Id` (UUID)

### Multi-Tenancy
- Users own "Shops"
- Every Product belongs to a specific `shop_id`
- Products can be filtered by shop

### File Storage
- External **File Service** backed by MinIO
- Product Service receives **public URLs** for images
- No file upload handling in this service

## Database Schema

### Entities

1. **Brand**
   - `id` (UUID, PK)
   - `name` (String, unique)
   - `logo_url` (String)

2. **Category**
   - `id` (UUID, PK)
   - `title` (String)
   - `parent_id` (UUID, nullable) - Self-referencing for hierarchy

3. **Size**
   - `id` (UUID, PK)
   - `value` (String) - e.g., "XL", "42"

4. **Product**
   - `id` (UUID, PK)
   - `shop_id` (UUID, Not Null, Indexed)
   - `name` (String)
   - `description` (Text)
   - `price` (BigDecimal)
   - `category` (ManyToOne -> Category)
   - `brand` (ManyToOne -> Brand)
   - `image_urls` (ElementCollection/List<String>)
   - `is_active` (Boolean)
   - `created_by` (UUID)
   - `created_at`, `updated_at` (Audit fields)

5. **ProductSize** (Inventory)
   - `id` (UUID, PK)
   - `product` (ManyToOne -> Product)
   - `size` (ManyToOne -> Size)
   - `quantity_available` (Integer)

## API Endpoints

### Brand Endpoints
- `POST /api/brands` - Create brand
- `GET /api/brands` - Get all brands
- `GET /api/brands/{id}` - Get brand by ID
- `PUT /api/brands/{id}` - Update brand
- `DELETE /api/brands/{id}` - Delete brand

### Category Endpoints
- `POST /api/categories` - Create category
- `GET /api/categories` - Get all categories
- `GET /api/categories/{id}` - Get category by ID
- `GET /api/categories/root` - Get root categories
- `GET /api/categories/parent/{parentId}` - Get subcategories
- `PUT /api/categories/{id}` - Update category
- `DELETE /api/categories/{id}` - Delete category

### Size Endpoints
- `POST /api/sizes` - Create size
- `GET /api/sizes` - Get all sizes
- `GET /api/sizes/{id}` - Get size by ID
- `PUT /api/sizes/{id}` - Update size
- `DELETE /api/sizes/{id}` - Delete size

### Product Endpoints
- `POST /api/products` - Create product (requires `X-User-Id` header)
- `GET /api/products` - Get all products
- `GET /api/products?shopId={shopId}` - Get products by shop
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/shop/{shopId}/active` - Get active products by shop
- `GET /api/products/search` - Search products (supports filters)
- `PUT /api/products/{id}` - Update product (requires `X-User-Id` header)
- `DELETE /api/products/{id}` - Delete product

### Product Size Endpoints
- `POST /api/product-sizes` - Create product size
- `GET /api/product-sizes/{id}` - Get product size by ID
- `GET /api/product-sizes/product/{productId}` - Get sizes for product
- `PUT /api/product-sizes/{id}` - Update product size
- `DELETE /api/product-sizes/{id}` - Delete product size

## RabbitMQ Integration

### Stock Decrease Queue
- **Queue Name:** `stock.decrease`
- **Payload:**
  ```json
  {
    "productId": "uuid",
    "sizeId": "uuid",
    "quantity": 1
  }
  ```
- **Behavior:** Decreases stock with pessimistic locking for thread safety

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=productdb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Server
SERVER_PORT=8083

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
java -jar target/product-service-1.0.0.jar
```

## Database Migration

Flyway is used for database migrations. Migrations are located in:
```
src/main/resources/db/migration/
```

Migrations run automatically on application startup.

## Development Notes

- All primary keys use UUID
- `shop_id` is indexed for performance
- Stock decrease operations use pessimistic locking
- JPA Auditing is enabled for `created_at` and `updated_at` fields
- MapStruct handles DTO-Entity mapping
- Global exception handler provides consistent error responses
