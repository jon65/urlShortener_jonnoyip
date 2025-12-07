# Spring Boot Backend Design Document
## URL Shortener - Java Backend Architecture

---

## 1. Project Structure

### 1.1 Maven/Gradle Project Layout

```
urlShortener_jonnoyip/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── jonnoyip/
│   │   │           └── urlshortener/
│   │   │               ├── UrlShortenerApplication.java
│   │   │               │
│   │   │               ├── config/                    # Configuration classes
│   │   │               │   ├── WebConfig.java
│   │   │               │   ├── CacheConfig.java
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   └── DatabaseConfig.java
│   │   │               │
│   │   │               ├── controller/                # REST Controllers
│   │   │               │   ├── UrlController.java
│   │   │               │   ├── RedirectController.java
│   │   │               │   └── HealthController.java
│   │   │               │
│   │   │               ├── service/                   # Business Logic
│   │   │               │   ├── UrlShorteningService.java
│   │   │               │   ├── UrlValidationService.java
│   │   │               │   ├── ShortCodeGenerator.java
│   │   │               │   └── CacheService.java
│   │   │               │
│   │   │               ├── repository/                # Data Access
│   │   │               │   └── UrlMappingRepository.java
│   │   │               │
│   │   │               ├── model/                     # Domain Models
│   │   │               │   ├── entity/
│   │   │               │   │   └── UrlMapping.java
│   │   │               │   ├── dto/
│   │   │               │   │   ├── ShortenUrlRequest.java
│   │   │               │   │   ├── ShortenUrlResponse.java
│   │   │               │   │   └── UrlInfoResponse.java
│   │   │               │   └── exception/
│   │   │               │       ├── UrlNotFoundException.java
│   │   │               │       ├── InvalidUrlException.java
│   │   │               │       └── ShortCodeCollisionException.java
│   │   │               │
│   │   │               ├── util/                      # Utilities
│   │   │               │   ├── Base62Encoder.java
│   │   │               │   └── UrlValidator.java
│   │   │               │
│   │   │               └── exception/                 # Global Exception Handling
│   │   │                   ├── GlobalExceptionHandler.java
│   │   │                   └── ErrorResponse.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/
│   │           └── migration/                        # Flyway/Liquibase migrations
│   │               └── V1__create_url_mappings_table.sql
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── jonnoyip/
│                   └── urlshortener/
│                       ├── controller/
│                       ├── service/
│                       └── repository/
│
├── pom.xml (or build.gradle)
├── Dockerfile
└── README.md
```

---

## 2. Layer Architecture

### 2.1 Three-Layer Architecture

```
┌─────────────────────────────────────┐
│      Controller Layer               │  ← REST API endpoints
│  (URLController, RedirectController)│
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│      Service Layer                  │  ← Business logic
│  (UrlShorteningService, etc.)       │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│      Repository Layer               │  ← Data access
│  (UrlMappingRepository - JPA)       │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│      Database (PostgreSQL)          │
└─────────────────────────────────────┘
```

### 2.2 Layer Responsibilities

**Controller Layer:**
- Handle HTTP requests/responses
- Request validation
- Response formatting
- HTTP status codes
- No business logic

**Service Layer:**
- Business logic implementation
- Transaction management
- Orchestration of multiple operations
- Caching logic
- Exception handling

**Repository Layer:**
- Database operations
- CRUD operations
- Query methods
- Data persistence

---

## 3. Core Components Design

### 3.1 Entity Model

**UrlMapping.java** (JPA Entity)
```java
// Key responsibilities:
- Map to database table
- Define relationships
- JPA annotations
- Validation constraints
```

**Fields:**
- `id` (Long, Primary Key, Auto-generated)
- `shortCode` (String, Unique, Indexed)
- `originalUrl` (String, Not Null)
- `createdAt` (LocalDateTime)
- `expiresAt` (LocalDateTime, Optional)
- `clickCount` (Long, Default 0)
- `isActive` (Boolean, Default true)

**Annotations:**
- `@Entity`, `@Table(name = "url_mappings")`
- `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `@Column(unique = true, nullable = false)`
- `@Index(name = "idx_short_code", columnList = "short_code")`
- `@CreationTimestamp`, `@UpdateTimestamp`

### 3.2 DTOs (Data Transfer Objects)

**ShortenUrlRequest.java**
- `url` (String, Required, Validated)
- `customCode` (String, Optional)
- `expiresInDays` (Integer, Optional)

**ShortenUrlResponse.java**
- `shortUrl` (String)
- `shortCode` (String)
- `originalUrl` (String)
- `createdAt` (LocalDateTime)

**UrlInfoResponse.java**
- `originalUrl` (String)
- `shortCode` (String)
- `createdAt` (LocalDateTime)
- `clickCount` (Long)
- `isActive` (Boolean)

### 3.3 Service Layer Components

#### 3.3.1 UrlShorteningService (Main Service)

**Responsibilities:**
- Orchestrate URL shortening process
- Coordinate between validation, code generation, and persistence
- Handle custom short codes
- Manage transactions

**Key Methods:**
- `shortenUrl(ShortenUrlRequest request): ShortenUrlResponse`
- `getOriginalUrl(String shortCode): String`
- `getUrlInfo(String shortCode): UrlInfoResponse`
- `deactivateUrl(String shortCode): void`
- `incrementClickCount(String shortCode): void`

**Dependencies:**
- `UrlMappingRepository`
- `ShortCodeGenerator`
- `UrlValidationService`
- `CacheService`

#### 3.3.2 ShortCodeGenerator

**Responsibilities:**
- Generate unique short codes
- Base62 encoding logic
- Handle collisions (if using random generation)
- Validate custom codes

**Key Methods:**
- `generateShortCode(): String` (auto-generate)
- `generateShortCode(Long id): String` (from ID)
- `isValidShortCode(String code): boolean`
- `decodeShortCode(String code): Long` (for Base62)

**Strategy Pattern:**
- Interface: `ShortCodeGenerator`
- Implementation: `Base62ShortCodeGenerator`
- Future: `RandomShortCodeGenerator`

#### 3.3.3 UrlValidationService

**Responsibilities:**
- Validate URL format
- Security checks (private IPs, localhost)
- URL accessibility check (optional)
- Sanitize URLs

**Key Methods:**
- `validateUrl(String url): void` (throws InvalidUrlException)
- `isValidFormat(String url): boolean`
- `isSafeUrl(String url): boolean`
- `sanitizeUrl(String url): String`

**Validation Rules:**
- Must start with http:// or https://
- Valid URL format (RFC 3986)
- Max length: 2048 characters
- Block private IP ranges
- Block localhost

#### 3.3.4 CacheService

**Responsibilities:**
- Cache URL lookups
- Cache eviction
- Cache statistics

**Key Methods:**
- `get(String shortCode): Optional<String>`
- `put(String shortCode, String originalUrl): void`
- `evict(String shortCode): void`
- `clear(): void`

**Implementation:**
- Use Caffeine cache (in-memory)
- TTL: 1 hour
- Max size: 10,000 entries
- LRU eviction policy

### 3.4 Repository Layer

**UrlMappingRepository.java** (Spring Data JPA)

**Interface extending:**
- `JpaRepository<UrlMapping, Long>`

**Custom Query Methods:**
- `Optional<UrlMapping> findByShortCode(String shortCode)`
- `Optional<UrlMapping> findByShortCodeAndIsActiveTrue(String shortCode)`
- `boolean existsByShortCode(String shortCode)`
- `List<UrlMapping> findByIsActiveFalseAndCreatedAtBefore(LocalDateTime date)`

**Query Optimization:**
- Use `@Query` for complex queries
- Use `@EntityGraph` for eager loading if needed
- Use `@Modifying` for bulk operations

### 3.5 Controller Layer

#### 3.5.1 UrlController

**Endpoints:**
- `POST /api/v1/shorten` - Create short URL
- `GET /api/v1/info/{shortCode}` - Get URL info

**Responsibilities:**
- Accept HTTP requests
- Validate request body
- Call service layer
- Format responses
- Handle exceptions

**Annotations:**
- `@RestController`
- `@RequestMapping("/api/v1")`
- `@Valid` on request bodies
- `@ResponseStatus` for status codes

#### 3.5.2 RedirectController

**Endpoints:**
- `GET /{shortCode}` - Redirect to original URL

**Responsibilities:**
- Handle redirect requests
- Lookup original URL
- Return HTTP 301 redirect
- Handle 404 cases

**Special Considerations:**
- Root path mapping (`@GetMapping("/{shortCode}")`)
- Must not conflict with static resources
- Fast response time critical

#### 3.5.3 HealthController

**Endpoints:**
- `GET /health` - Health check
- `GET /actuator/health` - Spring Boot Actuator (alternative)

**Checks:**
- Application status
- Database connectivity
- Cache status (if applicable)

---

## 4. Configuration Classes

### 4.1 WebConfig

**Purpose:** Web MVC configuration

**Configurations:**
- CORS settings
- Content negotiation
- Exception resolvers
- Interceptors (for logging, rate limiting)

**Key Annotations:**
- `@Configuration`
- `@EnableWebMvc` (if needed)
- `@EnableCaching`

### 4.2 CacheConfig

**Purpose:** Cache configuration

**Configuration:**
- Caffeine cache bean
- Cache manager
- Cache specifications (TTL, size, eviction)

**Key Components:**
- `CaffeineCacheManager`
- `Cache<String, String>` bean for URL lookups

### 4.3 DatabaseConfig

**Purpose:** Database and JPA configuration

**Configurations:**
- JPA properties
- HikariCP connection pool settings
- Transaction management
- Flyway/Liquibase integration

### 4.4 SecurityConfig (Future Enhancement)

**Purpose:** Security configuration

**Configurations:**
- Rate limiting
- CORS policies
- Request validation
- API key authentication (future)

---

## 5. Exception Handling

### 5.1 Custom Exceptions

**UrlNotFoundException**
- Thrown when short code doesn't exist
- HTTP 404

**InvalidUrlException**
- Thrown when URL validation fails
- HTTP 400

**ShortCodeCollisionException**
- Thrown when custom code already exists
- HTTP 409 Conflict

**RateLimitExceededException**
- Thrown when rate limit exceeded
- HTTP 429

### 5.2 GlobalExceptionHandler

**Purpose:** Centralized exception handling

**Annotations:**
- `@RestControllerAdvice`
- `@ExceptionHandler` for each exception type

**Response Format:**
```json
{
  "error": "Error type",
  "message": "Error message",
  "timestamp": "2024-01-01T00:00:00",
  "path": "/api/v1/shorten"
}
```

**Key Methods:**
- `handleUrlNotFoundException()`
- `handleInvalidUrlException()`
- `handleValidationException()`
- `handleGenericException()`

---

## 6. Dependencies (Maven)

### 6.1 Core Dependencies

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Starter Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Boot Starter Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Spring Boot Starter Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Caffeine Cache -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Spring Boot Actuator (Monitoring) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- SpringDoc OpenAPI (Swagger) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- Flyway (Database Migration) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### 6.2 Test Dependencies

```xml
<!-- Spring Boot Starter Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers (for integration tests) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 7. Application Properties

### 7.1 application.properties (Base)

```properties
# Application
spring.application.name=url-shortener
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshortener
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:password}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Connection Pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# Cache (Caffeine)
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=1h

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized

# Logging
logging.level.com.jonnoyip.urlshortener=INFO
logging.level.org.springframework.web=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Custom Properties
app.short-url.domain=https://jonnoyip.com
app.short-url.code-length=6
app.url.max-length=2048
app.rate-limit.requests-per-minute=100
```

### 7.2 application-prod.properties

```properties
# Production overrides
spring.datasource.url=${RDS_URL}
spring.datasource.username=${RDS_USERNAME}
spring.datasource.password=${RDS_PASSWORD}

logging.level.root=WARN
logging.level.com.jonnoyip.urlshortener=INFO

# Disable dev features
spring.jpa.show-sql=false
```

---

## 8. Design Patterns

### 8.1 Service Layer Pattern
- Business logic encapsulated in services
- Controllers delegate to services
- Services coordinate repositories

### 8.2 Repository Pattern
- Spring Data JPA provides abstraction
- Custom queries when needed
- Separation of data access logic

### 8.3 DTO Pattern
- Separate entities from API contracts
- Prevent entity exposure
- Version API independently

### 8.4 Strategy Pattern
- ShortCodeGenerator interface
- Multiple implementations (Base62, Random)
- Easy to swap strategies

### 8.5 Factory Pattern (Optional)
- Create ShortCodeGenerator based on config
- Future: Different generators for different use cases

### 8.6 Builder Pattern (Optional)
- For complex DTOs
- Fluent API for object construction

---

## 9. Transaction Management

### 9.1 Transaction Boundaries

**Service Layer:**
- `@Transactional` on service methods
- Read-only transactions for queries
- Read-write for mutations

**Key Methods:**
- `@Transactional` - `shortenUrl()`
- `@Transactional(readOnly = true)` - `getOriginalUrl()`
- `@Transactional` - `incrementClickCount()`

### 9.2 Transaction Isolation
- Default: READ_COMMITTED
- Consider SERIALIZABLE for short code generation (if using random)

---

## 10. Caching Strategy

### 10.1 Cache Layers

**Level 1: Service Method Caching**
- `@Cacheable` on `getOriginalUrl()`
- Cache key: shortCode
- Cache value: originalUrl

**Level 2: Manual Cache (Caffeine)**
- Direct cache access in service
- More control over cache operations
- Custom eviction policies

### 10.2 Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("urlCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats());
        return cacheManager;
    }
}
```

### 10.3 Cache Usage

**In Service:**
```java
@Cacheable(value = "urlCache", key = "#shortCode")
public String getOriginalUrl(String shortCode) {
    // Database lookup
}
```

---

## 11. Validation

### 11.1 Request Validation

**Bean Validation (JSR-303):**
- `@Valid` on controller parameters
- `@NotNull`, `@NotBlank`, `@URL` on DTOs
- Custom validators for complex rules

**Custom Validators:**
- `@ValidUrl` - Custom URL validation
- `@ValidShortCode` - Short code format validation

### 11.2 Validation Flow

```
Controller → @Valid → DTO Validation → Service → Business Validation
```

---

## 12. Logging Strategy

### 12.1 Logging Levels

- **ERROR:** Exceptions, critical failures
- **WARN:** Validation failures, rate limit exceeded
- **INFO:** API requests, URL creation, redirects
- **DEBUG:** Detailed flow (dev only)

### 12.2 Structured Logging

**Use MDC (Mapped Diagnostic Context):**
- Request ID
- User IP
- Short code
- Timestamp

**Log Format:**
```
[INFO] [request-id:abc123] [ip:192.168.1.1] Short URL created: abc123 -> https://example.com
```

### 12.3 Logging Points

- Incoming API requests
- URL shortening operations
- Redirect operations
- Cache hits/misses
- Database queries (if enabled)
- Exceptions

---

## 13. Testing Strategy

### 13.1 Unit Tests

**Service Layer:**
- Mock repositories
- Test business logic
- Test edge cases

**Utility Classes:**
- Base62Encoder tests
- UrlValidator tests

### 13.2 Integration Tests

**Repository Tests:**
- Use Testcontainers for PostgreSQL
- Test database operations
- Test custom queries

**Controller Tests:**
- MockMvc for REST endpoints
- Test request/response
- Test exception handling

### 13.3 Test Structure

```
test/
├── unit/
│   ├── service/
│   └── util/
├── integration/
│   ├── repository/
│   └── controller/
└── e2e/
    └── UrlShortenerE2ETest.java
```

---

## 14. API Documentation

### 14.1 SpringDoc OpenAPI (Swagger)

**Configuration:**
- Auto-generate API docs
- Interactive API testing
- Request/response schemas

**Access:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

**Annotations:**
- `@Operation` - Endpoint description
- `@ApiResponse` - Response documentation
- `@Schema` - Model documentation

---

## 15. Performance Considerations

### 15.1 Database Optimization

- Index on `short_code` column
- Connection pooling (HikariCP)
- Prepared statements (automatic with JPA)
- Batch operations for bulk inserts

### 15.2 Caching

- Cache frequently accessed URLs
- Cache statistics for monitoring
- Cache warming (optional)

### 15.3 Async Processing

- Async URL validation (optional)
- Async analytics updates
- `@Async` for non-blocking operations

### 15.4 Response Compression

- Enable gzip compression
- Reduce payload size
- Faster response times

---

## 16. Security Considerations

### 16.1 Input Validation

- Validate all inputs
- Sanitize URLs
- Prevent SQL injection (JPA handles this)
- Prevent XSS in responses

### 16.2 Rate Limiting (Future)

- Implement rate limiting filter
- Per IP address
- Configurable limits

### 16.3 HTTPS

- Enforce HTTPS in production
- Redirect HTTP to HTTPS
- Secure cookies (if added)

---

## 17. Monitoring & Observability

### 17.1 Spring Boot Actuator

**Endpoints:**
- `/actuator/health` - Health check
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application info

### 17.2 Custom Metrics

- URL creation count
- Redirect count
- Cache hit rate
- Average response time
- Error rate

### 17.3 Health Checks

- Database connectivity
- Cache status
- Disk space
- Memory usage

---

## 18. Deployment Considerations

### 18.1 Dockerfile

**Multi-stage build:**
- Build stage: Maven build
- Runtime stage: JRE only

**Key Points:**
- Use official OpenJDK image
- Copy JAR file
- Expose port 8080
- Non-root user

### 18.2 Environment Variables

- Database credentials
- Domain name
- Cache configuration
- Logging level

### 18.3 JVM Tuning

- Heap size configuration
- GC settings
- Memory limits

---

## 19. Code Quality

### 19.1 Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Keep methods small and focused
- Add JavaDoc for public APIs

### 19.2 Static Analysis

- Use Checkstyle or SpotBugs
- SonarQube integration (optional)
- Pre-commit hooks

### 19.3 Code Review Checklist

- Proper exception handling
- Transaction boundaries
- Cache usage
- Logging
- Test coverage

---

## 20. Development Workflow

### 20.1 Local Development

1. Start PostgreSQL (Docker)
2. Run Flyway migrations
3. Start Spring Boot application
4. Test via Swagger UI

### 20.2 Database Migrations

- Use Flyway for version control
- Naming: `V{version}__{description}.sql`
- Test migrations before production

### 20.3 Feature Development

1. Create feature branch
2. Write tests first (TDD)
3. Implement feature
4. Run all tests
5. Code review
6. Merge to main

---

## 21. Key Implementation Details

### 21.1 Base62 Encoding

**Algorithm:**
- Characters: 0-9, a-z, A-Z (62 characters)
- Convert decimal ID to Base62
- Reverse for decoding

**Example:**
- ID 1 → "1"
- ID 62 → "10"
- ID 3844 → "100"

### 21.2 Short Code Generation Flow

```
1. Save UrlMapping entity (auto-generate ID)
2. Get generated ID from entity
3. Encode ID to Base62
4. Update entity with shortCode
5. Return shortCode
```

### 21.3 Redirect Flow

```
1. Receive GET /{shortCode}
2. Check cache for shortCode
3. If cache miss, query database
4. If found, cache result and return 301 redirect
5. If not found, return 404
```

---

## 22. Future Enhancements

### 22.1 Phase 2 Features

- Custom short codes
- URL expiration
- Analytics tracking
- User accounts
- API authentication

### 22.2 Technical Improvements

- Redis caching
- Message queue for async tasks
- Distributed tracing
- Advanced monitoring
- A/B testing framework

---

## 23. Best Practices Summary

1. **Separation of Concerns:** Clear layer boundaries
2. **Single Responsibility:** Each class has one job
3. **Dependency Injection:** Use Spring's DI
4. **Exception Handling:** Centralized exception handling
5. **Validation:** Validate at controller and service layers
6. **Caching:** Cache read-heavy operations
7. **Transactions:** Proper transaction boundaries
8. **Logging:** Structured, meaningful logs
9. **Testing:** High test coverage
10. **Documentation:** API and code documentation

---

**Document Version:** 1.0  
**Last Updated:** [Current Date]  
**Related Documents:** SYSTEM_DESIGN.md

