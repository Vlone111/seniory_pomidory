# Shop Service

Shop Service for the Marketplace Microservices system.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.7
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA (Hibernate)
- **Tools:** Lombok, MapStruct, Flyway
- **Build Tool:** Gradle

## Features

- **Shop Management:** Full CRUD operations for shops
- **Owner-based Access Control:** Only shop owners can update/delete their shops
- **Unique Constraints:** Shop names and URLs must be unique
- **Shop Discovery:** Find shops by ID, URL, or owner
- **X-User-Id Security:** Uses header-based user identification from KrakenD Gateway

## Database Schema

### Shop Entity

- `id` (UUID, PK)
- `shop_name` (String, Unique, Not Null)
- `shop_url` (String, Unique, Not Null) - URL-friendly identifier
- `description` (Text)
- `pfp_url` (String) - Profile picture URL from File Service
- `design_code` (String, Not Null) - Theme/design identifier
- `owner_id` (UUID, Indexed, Not Null) - User who owns the shop
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

## API Endpoints

### Shop Endpoints

- `POST /api/shops` - Create a new shop
  - **Headers:** `X-User-Id` (UUID)
  - **Request Body:**
    ```json
    {
      "shopName": "My Awesome Shop",
      "shopUrl": "my-awesome-shop",
      "description": "Best products ever",
      "pfpUrl": "https://...",
      "designCode": "theme-modern"
    }
    ```
  - **Validations:**
    - Shop name is required
    - Shop URL is required and must be lowercase alphanumeric with hyphens only
    - Design code is required

- `GET /api/shops/{shopId}` - Get shop by ID

- `GET /api/shops/url/{shopUrl}` - Get shop by URL (e.g., `/api/shops/url/my-awesome-shop`)

- `GET /api/shops/owner/{ownerId}` - Get all shops owned by a specific user

- `GET /api/shops/my-shops` - Get all shops owned by the authenticated user
  - **Headers:** `X-User-Id` (UUID)

- `GET /api/shops` - Get all shops (public listing)

- `PUT /api/shops/{shopId}` - Update shop
  - **Headers:** `X-User-Id` (UUID)
  - **Authorization:** Only the shop owner can update
  - **Request Body:**
    ```json
    {
      "shopName": "Updated Name",
      "description": "Updated description",
      "pfpUrl": "https://...",
      "designCode": "theme-dark"
    }
    ```
  - **Note:** Shop URL cannot be changed after creation

- `DELETE /api/shops/{shopId}` - Delete shop
  - **Headers:** `X-User-Id` (UUID)
  - **Authorization:** Only the shop owner can delete

## Business Rules

1. **Unique Shop URL:** Each shop must have a unique URL identifier
2. **Unique Shop Name:** Shop names must be unique across the platform
3. **Owner Authorization:** Only the shop owner can update or delete their shop
4. **URL Immutability:** Shop URLs cannot be changed after creation (for SEO and link stability)
5. **Required Fields:** Shop name, shop URL, and design code are mandatory

## Security

- Authentication handled by **KrakenD API Gateway**
- User ID passed via `X-User-Id` header (UUID)
- Owner validation on UPDATE and DELETE operations
- Never trust user ID from request body

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shop_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Server
SERVER_PORT=8080

# Logging
LOG_LEVEL=INFO
SHOW_SQL=false
```

## Running the Application

### Prerequisites
- Java 21
- PostgreSQL
- Gradle

### Build
```bash
./gradlew clean build
```

### Run
```bash
./gradlew bootRun
```

Or with JAR:
```bash
java -jar build/libs/shop-0.0.1-SNAPSHOT.jar
```

## Database Migration

Flyway is used for database migrations. Migrations are located in:
```
src/main/resources/db/migration/
```

Migrations run automatically on application startup.

## Key Improvements Made

✅ **Changed ID from Long to UUID** - Consistent with other microservices  
✅ **Added Flyway migrations** - Replaced dangerous `ddl-auto=update`  
✅ **Added validation** - Request DTOs have proper validation annotations  
✅ **Complete CRUD** - All endpoints (CREATE, READ, UPDATE, DELETE)  
✅ **Response DTOs** - Returns proper response objects  
✅ **MapStruct** - Automatic DTO-Entity mapping  
✅ **Exception handling** - GlobalExceptionHandler with proper error responses  
✅ **Owner authorization** - Only owners can modify their shops  
✅ **Duplicate checks** - Prevents duplicate shop names/URLs  
✅ **Repository improvements** - Renamed from `Shopdao` to `ShopRepository`  
✅ **Query methods** - Find by owner, URL, name  
✅ **JPA Auditing** - Automatic `created_at` and `updated_at` timestamps  
✅ **Configuration** - Converted to YAML with environment variables  

## RabbitMQ Assessment

**RabbitMQ is NOT needed** in the Shop Service currently because:
- Shop operations are synchronous
- Other services reference `shop_id` but don't need real-time notifications
- No event-driven workflows required

**Future considerations:**
- Could publish `shop.created` events for analytics
- Could publish `shop.updated` events if needed by other services
- But for now, it's not necessary

## Development Notes

- All primary keys use UUID
- `owner_id` is indexed for performance
- Shop URL cannot be changed after creation
- MapStruct handles DTO-Entity mapping
- Global exception handler provides consistent error responses
- JPA Auditing enabled for timestamps
