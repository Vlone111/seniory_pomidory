# News Service

News Service for the Marketplace Microservices system - manages public content including news articles, promotions, and announcements.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA (Hibernate)
- **Tools:** Lombok, MapStruct, Flyway

## Features

- **Public News Listing:** Fast, paginated access to published articles
- **SEO-Friendly URLs:** Automatic slug generation from titles
- **Admin Management:** Full CRUD operations for administrators
- **Soft Delete:** Articles can be soft-deleted and restored
- **Publishing Workflow:** Draft → Published with timestamp tracking
- **Image Integration:** Receives public URLs from File Service (MinIO)
- **HTML/Markdown Support:** Content field supports rich text

## Database Schema

### News Entity

- `id` (UUID, PK)
- `title` (String, Not Null)
- `content` (Text) - Supports HTML or Markdown
- `slug` (String, Unique) - URL-friendly identifier (e.g., "summer-sale-2024")
- `preview_image_url` (String) - URL from File Service
- `is_published` (Boolean, default false)
- `published_at` (Timestamp, nullable)
- `created_by` (UUID) - Admin ID from X-User-Id header
- `created_at`, `updated_at` (Audit fields)
- `deleted` (Boolean) - Soft delete flag

## API Endpoints

### Public Endpoints (No Authentication Required)

- `GET /api/news` - Get paginated list of published news
  - **Query Params:** `page` (default: 0), `size` (default: 10)
  - **Response:** Paginated list sorted by `published_at` DESC
  - **Example:** `/api/news?page=0&size=20`

- `GET /api/news/{slug}` - Get specific article by slug
  - **Example:** `/api/news/summer-sale-2024`
  - **Returns:** Full article details (only if published)

### Admin Endpoints (Require X-User-Id Header)

#### Create & Update
- `POST /api/news` - Create news article
  - **Headers:** `X-User-Id` (UUID)
  - **Request Body:**
    ```json
    {
      "title": "Summer Sale 2024",
      "content": "<p>Amazing discounts...</p>",
      "slug": "summer-sale-2024",
      "previewImageUrl": "http://localhost:9000/bucket/image.jpg",
      "isPublished": false
    }
    ```
  - **Note:** `slug` is optional - auto-generated from title if not provided

- `PUT /api/news/{id}` - Update news article
  - **Headers:** `X-User-Id` (UUID)
  - **Request Body:** Same as create

#### Read Operations
- `GET /api/news/admin/all` - Get all news (including unpublished)
  - **Headers:** `X-User-Id` (UUID)
  - **Query Params:** `page`, `size`

- `GET /api/news/admin/filter` - Filter by published status
  - **Headers:** `X-User-Id` (UUID)
  - **Query Params:** `isPublished` (true/false/null), `page`, `size`
  - **Example:** `/api/news/admin/filter?isPublished=false&page=0&size=10`

- `GET /api/news/admin/slug/{slug}` - Get by slug (including unpublished)
  - **Headers:** `X-User-Id` (UUID)

- `GET /api/news/admin/{id}` - Get by ID
  - **Headers:** `X-User-Id` (UUID)

#### Delete Operations
- `DELETE /api/news/{id}` - Soft delete article
  - **Headers:** `X-User-Id` (UUID)
  - **Note:** Article is marked as deleted but remains in database

- `DELETE /api/news/admin/{id}/permanent` - Permanently delete article
  - **Headers:** `X-User-Id` (UUID)
  - **Warning:** Irreversible operation

## Business Logic

### Slug Generation
- Automatically generated from title if not provided
- Converts to lowercase
- Removes special characters
- Replaces spaces with hyphens
- Example: "New Collection 2024!" → "new-collection-2024"

### Publishing Workflow
1. **Draft State:** `isPublished = false`, `publishedAt = null`
2. **Publishing:** When `isPublished` set to `true`, `publishedAt` is automatically set to current timestamp
3. **Unpublishing:** When `isPublished` set to `false`, `publishedAt` is cleared

### Soft Delete
- Articles are marked as `deleted = true` instead of being removed
- Soft-deleted articles are excluded from all queries
- Can be permanently deleted via admin endpoint

## Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=newsdb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Server
SERVER_PORT=8085

# Logging
LOG_LEVEL=INFO
SHOW_SQL=false
```

## Running the Application

### Prerequisites
- Java 17+
- PostgreSQL
- Maven

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Or with JAR:
```bash
java -jar target/news-service-1.0.0.jar
```

## Database Migration

Flyway is used for database migrations. Migrations are located in:
```
src/main/resources/db/migration/
```

Migrations run automatically on application startup.

## Integration with Other Services

### File Service
- News Service receives `previewImageUrl` as a String
- URL format: `http://localhost:9000/bucket/filename.jpg`
- No direct file upload - handled by File Service

### Usage Flow
1. Admin uploads image to File Service
2. File Service returns public URL
3. Admin creates news article with the URL
4. Public users see the image when viewing the article

## Key Features

✅ **Automatic slug generation** - SEO-friendly URLs  
✅ **Pagination** - Efficient data loading  
✅ **Soft delete** - Data recovery possible  
✅ **Publishing workflow** - Draft → Published  
✅ **Duplicate slug prevention** - Unique constraint  
✅ **Timestamp tracking** - Created/updated/published dates  
✅ **Admin identification** - Tracks who created each article  
✅ **Public/Admin separation** - Different endpoints for different roles  
✅ **Rich content support** - HTML/Markdown in content field  

## Security Considerations

### Current Implementation
- Public endpoints are truly public (no auth)
- Admin endpoints require `X-User-Id` header
- No role-based access control (RBAC) implemented

### Production Recommendations
1. **Add RBAC:** Verify admin role via KrakenD Gateway
2. **Rate Limiting:** Prevent abuse of public endpoints
3. **Content Sanitization:** Sanitize HTML content to prevent XSS
4. **Audit Logging:** Track all admin operations
5. **API Gateway:** Route through KrakenD for authentication

## Performance Optimizations

### Database Indexes
- `slug` - Fast lookup by URL
- `is_published, published_at` - Optimized for public listing
- `created_at` - Admin listing performance
- `deleted` - Exclude soft-deleted records efficiently

### Query Optimization
- Pagination prevents loading all records
- Soft delete filter in queries
- Indexed columns for common queries

## Example Usage

### Create Draft Article
```bash
curl -X POST http://localhost:8085/api/news \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "title": "New Spring Collection",
    "content": "<h1>Check out our new collection!</h1>",
    "previewImageUrl": "http://localhost:9000/bucket/spring.jpg",
    "isPublished": false
  }'
```

### Publish Article
```bash
curl -X PUT http://localhost:8085/api/news/{id} \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "title": "New Spring Collection",
    "content": "<h1>Check out our new collection!</h1>",
    "previewImageUrl": "http://localhost:9000/bucket/spring.jpg",
    "isPublished": true
  }'
```

### Get Published News (Public)
```bash
curl http://localhost:8085/api/news?page=0&size=10
```

### Get Article by Slug (Public)
```bash
curl http://localhost:8085/api/news/new-spring-collection
```

## Development Notes

- All primary keys use UUID
- JPA Auditing enabled for timestamps
- MapStruct handles DTO-Entity mapping
- Global exception handler provides consistent error responses
- Slug generation happens in `@PrePersist` and `@PreUpdate`
- Published timestamp set automatically when publishing
