# URL Shortener System Design Document
## Domain: jonnoyip.com

---

## 1. Overview

This document outlines the system design for a URL shortener service that will shorten long URLs into compact, shareable links under the domain `jonnoyip.com`. The system will be built using Java and deployed on AWS infrastructure.

### 1.1 Purpose
- Convert long URLs into short, memorable links (e.g., `jonnoyip.com/abc123`)
- Redirect short URLs to their original long URLs
- Track basic analytics (optional for future phases)

### 1.2 Scope (Phase 1 - Basics)
- URL shortening and redirection
- Basic validation and error handling
- RESTful API for URL management
- Simple web interface for URL creation

---

## 2. Requirements

### 2.1 Functional Requirements
1. **URL Shortening**
   - Accept long URLs via API
   - Generate unique short codes (e.g., 6-8 characters)
   - Return shortened URL in format: `https://jonnoyip.com/{shortCode}`
   - Validate input URLs (format, accessibility)

2. **URL Redirection**
   - Accept requests to short URLs
   - Look up original URL from short code
   - Redirect user to original URL (HTTP 301/302)
   - Handle invalid/expired short codes

3. **URL Management**
   - Store mapping between short codes and original URLs
   - Support custom short codes (optional)
   - Prevent duplicate short codes

### 2.2 Non-Functional Requirements
1. **Performance**
   - Redirection latency: < 100ms (p95)
   - Support 1000+ requests per second (initial)
   - High availability (99.9% uptime)

2. **Scalability**
   - Horizontal scaling capability
   - Database can handle millions of URL mappings

3. **Security**
   - Validate and sanitize input URLs
   - Prevent malicious URLs (phishing, malware)
   - Rate limiting to prevent abuse
   - HTTPS only

4. **Reliability**
   - Data persistence
   - Error handling and logging
   - Graceful degradation

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │
       │ HTTPS
       │
┌──────▼─────────────────────────────────────┐
│         AWS Route 53 (DNS)                 │
│      jonnoyip.com → Load Balancer          │
└──────┬─────────────────────────────────────┘
       │
┌──────▼─────────────────────────────────────┐
│      Application Load Balancer (ALB)       │
└──────┬─────────────────────────────────────┘
       │
       ├─────────────────┬─────────────────┐
       │                 │                 │
┌──────▼──────┐   ┌──────▼──────┐   ┌──────▼──────┐
│   EC2/ECS   │   │   EC2/ECS   │   │   EC2/ECS   │
│  Instance   │   │  Instance   │   │  Instance   │
│  (Java App) │   │  (Java App) │   │  (Java App) │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
                ┌────────▼────────┐
                │   RDS/DynamoDB  │
                │   (Database)    │
                └─────────────────┘
```

### 3.2 Component Architecture

```
┌─────────────────────────────────────────────┐
│           Java Application Layer            │
├─────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │   REST   │  │  URL     │  │ Redirect │  │
│  │  API     │  │ Service  │  │ Service  │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │             │              │        │
│  ┌────▼─────────────▼──────────────▼─────┐ │
│  │         URL Shortening Logic          │ │
│  │  - Code Generation                    │ │
│  │  - Validation                         │ │
│  │  - Duplicate Checking                 │ │
│  └────┬──────────────────────────────────┘ │
│       │                                    │
│  ┌────▼──────────────────────────────────┐ │
│  │         Data Access Layer (DAO)       │ │
│  └────┬──────────────────────────────────┘ │
└───────┼────────────────────────────────────┘
        │
┌───────▼────────────────────────────────────┐
│         Database Layer                     │
│  - URL Mappings                            │
│  - Short Code Index                        │
└────────────────────────────────────────────┘
```

---

## 4. Core Components

### 4.1 URL Shortening Service
**Responsibilities:**
- Generate unique short codes
- Validate input URLs
- Check for duplicates
- Store URL mappings

**Short Code Generation Strategy:**
- **Option 1: Base62 Encoding** (Recommended for Phase 1)
  - Use auto-incrementing ID from database
  - Encode ID to Base62 (0-9, a-z, A-Z)
  - Example: ID 1 → "1", ID 62 → "10", ID 1000 → "g8"
  - Pros: Predictable, sequential, no collisions
  - Cons: Sequential IDs reveal usage patterns

- **Option 2: Random Generation**
  - Generate random 6-8 character strings
  - Check for collisions in database
  - Retry if collision occurs
  - Pros: Non-sequential, harder to guess
  - Cons: Requires collision checking

**Recommendation:** Start with Base62 encoding for simplicity and guaranteed uniqueness.

### 4.2 Redirect Service
**Responsibilities:**
- Lookup original URL from short code
- Handle HTTP redirects (301 Permanent or 302 Temporary)
- Cache frequently accessed URLs
- Handle invalid/expired codes

**Redirect Type:**
- **301 Permanent Redirect:** Better for SEO, browser caching
- **302 Temporary Redirect:** Better for analytics tracking
- **Recommendation:** Use 301 for Phase 1, can be configurable later

### 4.3 REST API
**Endpoints:**

1. **POST /api/v1/shorten**
   - Request: `{ "url": "https://example.com/very/long/url" }`
   - Response: `{ "shortUrl": "https://jonnoyip.com/abc123", "shortCode": "abc123" }`
   - Status Codes: 200 (success), 400 (invalid URL), 500 (server error)

2. **GET /api/v1/{shortCode}**
   - Redirects to original URL
   - Status Codes: 301/302 (redirect), 404 (not found)

3. **GET /api/v1/info/{shortCode}** (Optional for Phase 1)
   - Returns original URL without redirecting
   - Response: `{ "originalUrl": "...", "createdAt": "..." }`

### 4.4 Web Interface (Optional for Phase 1)
- Simple HTML form to create short URLs
- Display shortened URL
- Basic error messages

---

## 5. Database Design

### 5.1 Data Model

**Table: url_mappings**

| Column        | Type          | Constraints           | Description                    |
|---------------|---------------|-----------------------|--------------------------------|
| id            | BIGINT        | PRIMARY KEY, AUTO_INC | Internal ID                    |
| short_code    | VARCHAR(10)   | UNIQUE, NOT NULL      | Short code (e.g., "abc123")    |
| original_url  | TEXT          | NOT NULL              | Original long URL              |
| created_at    | TIMESTAMP     | NOT NULL, DEFAULT NOW | Creation timestamp             |
| expires_at    | TIMESTAMP     | NULL                  | Optional expiration (future)   |
| click_count   | BIGINT        | DEFAULT 0             | Analytics (future)             |
| is_active     | BOOLEAN       | DEFAULT TRUE          | Soft delete flag               |

**Indexes:**
- Primary key on `id`
- Unique index on `short_code` (for fast lookups)
- Index on `created_at` (for cleanup queries)

### 5.2 Database Choice

**Option 1: Amazon RDS (PostgreSQL/MySQL)**
- Pros: ACID compliance, SQL queries, relational data
- Cons: Scaling requires read replicas, connection pooling needed
- **Recommendation for Phase 1:** PostgreSQL on RDS

**Option 2: Amazon DynamoDB**
- Pros: Auto-scaling, low latency, serverless
- Cons: NoSQL, different query patterns, cost at scale
- **Recommendation for Phase 2+:** Consider for high-scale scenarios

**Phase 1 Recommendation:** Start with RDS PostgreSQL for simplicity and flexibility.

---

## 6. Technology Stack

### 6.1 Backend
- **Language:** Java 17+ (LTS)
- **Framework:** Spring Boot 3.x
  - Spring Web (REST API)
  - Spring Data JPA (Database access)
  - Spring Validation (Input validation)
- **Build Tool:** Maven or Gradle
- **HTTP Server:** Embedded Tomcat (Spring Boot default)

### 6.2 Database
- **Primary:** PostgreSQL 15+ (on RDS)
- **Connection Pooling:** HikariCP (Spring Boot default)

### 6.3 Caching (Future Enhancement)
- **Redis/ElastiCache:** For frequently accessed URL lookups
- **In-Memory Cache:** Caffeine (for Phase 1, simple caching)

### 6.4 Infrastructure
- **Compute:** 
  - Option A: EC2 instances (t3.medium or larger)
  - Option B: ECS Fargate (containerized, easier scaling)
- **Load Balancer:** Application Load Balancer (ALB)
- **DNS:** Route 53
- **SSL/TLS:** AWS Certificate Manager (ACM) for HTTPS
- **Monitoring:** CloudWatch

### 6.5 Development Tools
- **Testing:** JUnit 5, Mockito, Spring Boot Test
- **API Documentation:** SpringDoc OpenAPI (Swagger)
- **Logging:** Logback (Spring Boot default) + CloudWatch Logs

---

## 7. AWS Infrastructure Design

### 7.1 Route 53 Configuration
- **Domain:** jonnoyip.com
- **Record Type:** A (Alias) pointing to ALB
- **Subdomain:** Optional `api.jonnoyip.com` for API endpoints
- **SSL Certificate:** Request from ACM for `*.jonnoyip.com`

### 7.2 Network Architecture

```
Internet
   │
   ▼
Route 53 (jonnoyip.com)
   │
   ▼
Application Load Balancer (ALB)
   │
   ├─── Target Group 1 (Port 80/443)
   │    ├─── EC2/ECS Instance 1 (Java App)
   │    ├─── EC2/ECS Instance 2 (Java App)
   │    └─── EC2/ECS Instance 3 (Java App)
   │
   └─── Health Checks: /health endpoint
```

### 7.3 Security Groups
- **ALB Security Group:**
  - Inbound: 80 (HTTP), 443 (HTTPS) from 0.0.0.0/0
  - Outbound: All traffic

- **Application Security Group:**
  - Inbound: 8080 (or app port) from ALB security group only
  - Outbound: 5432 (PostgreSQL) to RDS security group

- **RDS Security Group:**
  - Inbound: 5432 from Application security group only
  - Outbound: None

### 7.4 Deployment Options

**Option A: EC2 Instances**
- Launch 2-3 EC2 instances (t3.medium)
- Install Java runtime
- Deploy JAR file
- Use Auto Scaling Group for scaling

**Option B: ECS Fargate (Recommended)**
- Containerize Java application (Docker)
- Deploy to ECS Fargate
- Auto-scaling based on CPU/memory
- Easier to manage and update

---

## 8. URL Validation & Security

### 8.1 URL Validation Rules
1. **Format Validation:**
   - Must start with `http://` or `https://`
   - Valid URL format (RFC 3986)
   - Maximum length: 2048 characters

2. **Security Checks:**
   - Block private IP ranges (10.x.x.x, 192.168.x.x, 127.0.0.1)
   - Block localhost URLs
   - Optional: Check against blacklist of known malicious domains
   - Rate limiting per IP (e.g., 100 requests/minute)

3. **Accessibility Check (Optional):**
   - Verify URL is reachable (HTTP HEAD request)
   - Timeout: 5 seconds
   - Can be async/background job

### 8.2 Input Sanitization
- URL encoding/decoding
- Prevent XSS in custom short codes
- SQL injection prevention (using parameterized queries)

---

## 9. Error Handling

### 9.1 Error Scenarios
1. **Invalid URL Format:** Return 400 Bad Request
2. **Short Code Not Found:** Return 404 Not Found
3. **Database Error:** Return 500 Internal Server Error
4. **Rate Limit Exceeded:** Return 429 Too Many Requests
5. **Service Unavailable:** Return 503 Service Unavailable

### 9.2 Logging Strategy
- Log all API requests (with sanitized URLs)
- Log errors with stack traces
- Log redirect requests (for analytics)
- Use structured logging (JSON format)
- Send logs to CloudWatch

---

## 10. Performance Optimization

### 10.1 Caching Strategy (Phase 1)
- **In-Memory Cache (Caffeine):**
  - Cache URL lookups by short code
  - TTL: 1 hour
  - Max size: 10,000 entries
  - Eviction: LRU (Least Recently Used)

### 10.2 Database Optimization
- Connection pooling (HikariCP default: 10 connections)
- Prepared statements (automatic with JPA)
- Index on `short_code` for fast lookups
- Consider read replicas for high read traffic

### 10.3 Application Optimization
- Async processing for non-critical tasks
- Compression for API responses (gzip)
- HTTP/2 support (via ALB)

---

## 11. Monitoring & Observability

### 11.1 Metrics to Track
- Request rate (requests/second)
- Response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Database connection pool usage
- Cache hit rate
- Redirect success rate

### 11.2 Alerts
- High error rate (> 5%)
- High response time (> 500ms p95)
- Database connection pool exhaustion
- Service downtime

### 11.3 Health Checks
- **Endpoint:** `/health`
- **Checks:**
  - Application is running
  - Database connectivity
  - Cache connectivity (if applicable)

---

## 12. Deployment Strategy

### 12.1 CI/CD Pipeline (Future)
1. **Source:** GitHub/GitLab
2. **Build:** Maven/Gradle build
3. **Test:** Unit tests, integration tests
4. **Package:** Docker image
5. **Deploy:** ECS Fargate or EC2

### 12.2 Initial Deployment (Phase 1)
- Manual deployment via AWS Console/CLI
- Blue-Green deployment strategy (future)

---

## 13. Future Enhancements (Post Phase 1)

1. **Analytics:**
   - Click tracking
   - Geographic data
   - Referrer tracking
   - Time-based analytics

2. **Advanced Features:**
   - Custom short codes
   - URL expiration
   - Password protection
   - QR code generation
   - Bulk URL shortening

3. **Scalability:**
   - Redis caching layer
   - Database sharding
   - CDN for static assets
   - Edge locations (CloudFront)

4. **Security:**
   - API authentication (JWT)
   - User accounts
   - URL blacklisting
   - CAPTCHA for public interface

---

## 14. Cost Estimation (AWS - Rough Estimates)

### Phase 1 (Low Traffic):
- **Route 53:** ~$0.50/month (hosted zone)
- **ALB:** ~$16/month (base cost)
- **EC2 (t3.medium x2):** ~$60/month
- **RDS (db.t3.micro):** ~$15/month
- **Data Transfer:** ~$10/month
- **Total:** ~$100-150/month

### Phase 2 (Medium Traffic):
- Add ElastiCache (Redis): +$15/month
- Scale EC2 instances: +$60/month
- **Total:** ~$200-250/month

---

## 15. Development Phases

### Phase 1: MVP (Current Focus)
- ✅ Basic URL shortening API
- ✅ URL redirection
- ✅ Database storage
- ✅ Simple web interface
- ✅ Basic validation
- ✅ AWS deployment

### Phase 2: Enhancement
- Analytics tracking
- Caching layer
- Custom short codes
- Rate limiting

### Phase 3: Scale
- High availability setup
- Advanced monitoring
- Performance optimization
- User accounts

---

## 16. API Design Summary

### Base URL
- Production: `https://jonnoyip.com`
- API: `https://jonnoyip.com/api/v1`

### Endpoints

| Method | Endpoint              | Description                    | Auth |
|--------|-----------------------|--------------------------------|------|
| POST   | /api/v1/shorten       | Create short URL               | No   |
| GET    | /{shortCode}          | Redirect to original URL       | No   |
| GET    | /api/v1/info/{code}   | Get URL info (no redirect)     | No   |
| GET    | /health               | Health check                   | No   |

---

## 17. Data Flow

### URL Shortening Flow
```
1. Client → POST /api/v1/shorten { "url": "..." }
2. API validates URL format
3. Service generates short code (Base62 encoding)
4. Check database for duplicate
5. Store mapping in database
6. Return short URL to client
```

### URL Redirection Flow
```
1. Client → GET /{shortCode}
2. Service looks up short code in cache
3. If not in cache, query database
4. If found, cache result and return 301 redirect
5. If not found, return 404
```

---

## 18. Risk Assessment

### Technical Risks
1. **Database becomes bottleneck:** Mitigation - Add caching, read replicas
2. **Short code collisions:** Mitigation - Use auto-increment + Base62 (guaranteed unique)
3. **Malicious URLs:** Mitigation - URL validation, blacklist checking
4. **High traffic spikes:** Mitigation - Auto-scaling, rate limiting

### Operational Risks
1. **Service downtime:** Mitigation - Multi-AZ deployment, health checks
2. **Data loss:** Mitigation - Automated backups, RDS snapshots
3. **Cost overruns:** Mitigation - Budget alerts, auto-scaling limits

---

## 19. Success Metrics

### Phase 1 Success Criteria
- System handles 1000+ requests/second
- 99.9% uptime
- < 100ms redirect latency (p95)
- Zero data loss
- Successful deployment on AWS

---

## 20. Next Steps

1. **Review and approve this design document**
2. **Set up AWS infrastructure:**
   - Route 53 hosted zone
   - RDS PostgreSQL instance
   - VPC and security groups
   - ALB setup
3. **Initialize Java project:**
   - Spring Boot project structure
   - Database schema creation
   - Basic API endpoints
4. **Implement core features:**
   - URL shortening logic
   - Redirect service
   - Database integration
5. **Testing:**
   - Unit tests
   - Integration tests
   - Load testing
6. **Deployment:**
   - Deploy to AWS
   - Configure DNS
   - SSL certificate setup

---

**Document Version:** 1.0  
**Last Updated:** [Current Date]  
**Author:** System Design Team

