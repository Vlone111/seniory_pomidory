# File Service

File Service for the Marketplace Microservices system using MinIO object storage.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.7
- **Storage:** MinIO (S3-compatible object storage)
- **Tools:** Lombok
- **Build Tool:** Gradle

## Features

- **File Upload:** Upload files with validation (size, type, extension)
- **File Download:** Download files by name
- **File Deletion:** Delete files from storage
- **File Listing:** List all files in the bucket
- **File Existence Check:** Check if a file exists
- **Public URLs:** Returns public URLs for uploaded files
- **Automatic Bucket Creation:** Creates bucket on startup if not exists
- **Public Read Policy:** Automatically applies public read policy to bucket

## Storage Architecture

### MinIO Integration
- **S3-Compatible:** Uses MinIO as S3-compatible object storage
- **Bucket:** All files stored in a single bucket (configurable)
- **Public Access:** Files are publicly accessible via URL
- **UUID Naming:** Files renamed with UUID to avoid conflicts

### URL Structure
```
http://localhost:9000/{bucket-name}/{uuid-filename.ext}
```

## API Endpoints

### File Operations

- `POST /api/files/upload` - Upload a file
  - **Request:** `multipart/form-data` with `file` parameter
  - **Response:**
    ```json
    {
      "fileUrl": "http://localhost:9000/fashionconstruct/uuid.jpg",
      "fileName": "uuid.jpg",
      "originalFileName": "photo.jpg",
      "fileSize": 123456,
      "contentType": "image/jpeg",
      "uploadedAt": "2026-01-08T07:30:00"
    }
    ```
  - **Validations:**
    - File not empty
    - File size within limit (default 10MB)
    - File extension allowed

- `GET /api/files/download/{fileName}` - Download a file
  - Returns file as attachment

- `DELETE /api/files/{fileName}` - Delete a file
  - Returns 204 No Content on success

- `GET /api/files` - List all files
  - Returns array of file information

- `GET /api/files/exists/{fileName}` - Check if file exists
  - Returns `true` or `false`

## File Validation

### Size Limits
- **Default:** 10MB per file
- **Configurable:** Via `file.max-size` property

### Allowed Extensions
- **Default:** `.jpg,.jpeg,.png,.gif,.webp,.svg,.pdf,.doc,.docx`
- **Configurable:** Via `file.allowed-extensions` property

### Validation Rules
1. File must not be empty
2. File size must be within limit
3. File extension must be in allowed list
4. File name must be valid

## Configuration

### Environment Variables

```bash
# Server
SERVER_PORT=8082

# MinIO Connection
MINIO_ENDPOINT=minio-storage
MINIO_PORT=9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=admin1234
MINIO_BUCKET_NAME=fashionconstruct
MINIO_PUBLIC_URL=http://localhost:9000
MINIO_SECURE=false

# File Upload Limits
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=10MB
FILE_MAX_SIZE=10485760
FILE_ALLOWED_EXTENSIONS=.jpg,.jpeg,.png,.gif,.webp,.svg,.pdf,.doc,.docx

# Logging
LOG_LEVEL=INFO
```

## Integration with Other Services

### Product Service
- Receives `imageUrls` (List<String>) in ProductRequestDto
- Stores URLs returned from File Service upload endpoint

### Shop Service
- Receives `pfpUrl` (String) in RegistrationRequest
- Stores URL returned from File Service upload endpoint

### Usage Pattern
1. Frontend uploads file to File Service
2. File Service returns public URL
3. Frontend sends URL to Product/Shop Service
4. Product/Shop Service stores URL as String

## Running the Application

### Prerequisites
- Java 21
- MinIO (running on port 9000)
- Gradle

### With Docker Compose
MinIO is typically started via docker-compose.yml:
```yaml
minio-storage:
  image: minio/minio
  ports:
    - "9000:9000"
    - "9001:9001"
  environment:
    MINIO_ROOT_USER: admin
    MINIO_ROOT_PASSWORD: admin1234
  command: server /data --console-address ":9001"
```

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
java -jar build/libs/fileservice-0.0.1-SNAPSHOT.jar
```

## MinIO Setup

### Automatic Setup
The service automatically:
1. Creates the bucket if it doesn't exist
2. Applies public read policy to the bucket
3. Logs setup status on startup

### Manual Access
- **Console:** http://localhost:9001
- **Username:** admin
- **Password:** admin1234

## Error Handling

### Exception Types
- `InvalidFileException` - File validation failed (400)
- `FileNotFoundException` - File not found (404)
- `FileStorageException` - Storage operation failed (500)
- `MaxUploadSizeExceededException` - File too large (413)

### Error Response Format
```json
{
  "timestamp": "2026-01-08T07:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "File extension '.exe' is not allowed",
  "path": "/api/files/upload"
}
```

## Key Improvements Made

✅ **Proper DTOs** - Structured response instead of plain String  
✅ **File validation** - Size, type, and extension checks  
✅ **Complete CRUD** - Upload, download, delete, list operations  
✅ **Exception handling** - GlobalExceptionHandler with proper errors  
✅ **Configurable URL** - `minio.public-url` instead of hardcoded localhost  
✅ **SLF4J logging** - Proper logging instead of System.err  
✅ **Naming conventions** - `fileConfig` → `MinioConfig`  
✅ **YAML configuration** - Environment variable support  
✅ **Validation dependency** - Spring Validation added  
✅ **File existence check** - Can verify if file exists  

## Security Considerations

### Current Setup (Development)
- Public read access to all files
- No authentication on upload
- Direct MinIO access

### Production Recommendations
1. **Authentication:** Add authentication to upload endpoint
2. **Authorization:** Verify user permissions before upload/delete
3. **API Gateway:** Route through KrakenD for security
4. **Private Buckets:** Use pre-signed URLs instead of public access
5. **Virus Scanning:** Add antivirus scanning for uploads
6. **Rate Limiting:** Prevent abuse of upload endpoint

## Compatibility Notes

### Other Services Expect String URLs
The File Service returns URLs in the response, but other services (Product, Shop) expect simple String URLs. The `fileUrl` field in the response maintains this compatibility:

```java
// Product Service expects:
List<String> imageUrls = ["http://localhost:9000/bucket/file1.jpg", ...]

// Shop Service expects:
String pfpUrl = "http://localhost:9000/bucket/logo.png"
```

The File Service response includes `fileUrl` which can be extracted and used directly.

## Development Notes

- Files are stored with UUID names to prevent conflicts
- Original filenames are preserved in metadata
- Public read policy allows direct browser access
- MinIO is S3-compatible, easy to migrate to AWS S3
- Bucket is created automatically on first startup
