# URL Shortener Service

A Spring Boot application for shortening URLs with support for local development and Google Cloud SQL production environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Development Setup](#development-setup)
- [Production Setup](#production-setup)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Database Migrations](#database-migrations)

## Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **PostgreSQL** (for local development)
- **Google Cloud SDK** (for production deployment, optional)

## Development Setup

### 1. Local Database Setup

Start a local PostgreSQL database:

```bash
# Using Docker
docker run --name urlshortener-db \
  -e POSTGRES_DB=urlshortener \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15

# Or using Homebrew (macOS)
brew services start postgresql@15
createdb urlshortener
```

### 2. Environment Variables (Optional)

Set custom database credentials if different from defaults:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

### 3. Run the Application

```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or set profile via environment variable
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/url-shortener-1.0.0.jar --spring.profiles.active=dev
```

### 4. Verify Application

- Application runs on: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- API documentation: `http://localhost:8080/swagger-ui.html` (if enabled)

### Development Features

- **SQL Logging**: All SQL queries are logged to console
- **Debug Logging**: Detailed application logs
- **Smaller Connection Pool**: Optimized for local development
- **Hot Reload**: Use Spring Boot DevTools for automatic restarts

## Production Setup

### Option 1: Google Cloud SQL with Unix Socket (Recommended for GCP)

Use this when deploying to:
- Google App Engine
- Cloud Run
- Google Compute Engine (GCE)

#### Step 1: Create Cloud SQL Instance

```bash
# Create PostgreSQL instance
gcloud sql instances create urlshortener-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1

# Create database
gcloud sql databases create urlshortener --instance=urlshortener-db

# Create user
gcloud sql users create app-user \
  --instance=urlshortener-db \
  --password=your-secure-password
```

#### Step 2: Configure Application

Update `application-prod.properties` to use Unix socket connection:

```properties
# Uncomment these lines:
spring.datasource.url=jdbc:postgresql:///${GCP_DB_NAME}?cloudSqlInstance=${GCP_CLOUD_SQL_CONNECTION_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory
spring.datasource.username=${GCP_DB_USERNAME}
spring.datasource.password=${GCP_DB_PASSWORD}
```

#### Step 3: Set Environment Variables

```bash
export GCP_CLOUD_SQL_CONNECTION_NAME="PROJECT_ID:REGION:INSTANCE_NAME"
export GCP_DB_NAME="urlshortener"
export GCP_DB_USERNAME="app-user"
export GCP_DB_PASSWORD="your-secure-password"
```

**Example:**
```bash
export GCP_CLOUD_SQL_CONNECTION_NAME="my-project:us-central1:urlshortener-db"
export GCP_DB_NAME="urlshortener"
export GCP_DB_USERNAME="app-user"
export GCP_DB_PASSWORD="secure-password-123"
```

#### Step 4: Run Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Option 2: Google Cloud SQL with TCP Connection

Use this when:
- Running locally but connecting to Cloud SQL
- Using Cloud SQL Proxy
- Connecting via public IP

#### Step 1: Set Up Cloud SQL Proxy (Recommended)

```bash
# Download Cloud SQL Proxy
curl -o cloud-sql-proxy https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.8.0/cloud-sql-proxy.darwin.arm64
chmod +x cloud-sql-proxy

# Start proxy (in a separate terminal)
./cloud-sql-proxy PROJECT_ID:REGION:INSTANCE_NAME --port=5432
```

#### Step 2: Set Environment Variables

```bash
export GCP_DB_URL="jdbc:postgresql://localhost:5432/urlshortener"
export GCP_DB_NAME="urlshortener"
export GCP_DB_USERNAME="app-user"
export GCP_DB_PASSWORD="your-secure-password"
```

**For Direct TCP Connection (Public IP):**
```bash
export GCP_DB_URL="jdbc:postgresql://PUBLIC_IP:5432/urlshortener"
export GCP_DB_NAME="urlshortener"
export GCP_DB_USERNAME="app-user"
export GCP_DB_PASSWORD="your-secure-password"
```

#### Step 3: Run Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Production Deployment

#### Build for Production

```bash
# Build JAR
mvn clean package -DskipTests

# The JAR will be in: target/url-shortener-1.0.0.jar
```

#### Deploy to Google Cloud Run

```bash
# Build container image
gcloud builds submit --tag gcr.io/PROJECT_ID/url-shortener

# Deploy to Cloud Run
gcloud run deploy url-shortener \
  --image gcr.io/PROJECT_ID/url-shortener \
  --platform managed \
  --region us-central1 \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod \
  --set-env-vars GCP_CLOUD_SQL_CONNECTION_NAME=PROJECT_ID:REGION:INSTANCE_NAME \
  --set-env-vars GCP_DB_NAME=urlshortener \
  --set-env-vars GCP_DB_USERNAME=app-user \
  --set-secrets GCP_DB_PASSWORD=db-password:latest \
  --add-cloudsql-instances PROJECT_ID:REGION:INSTANCE_NAME \
  --allow-unauthenticated
```

#### Deploy to Google App Engine

Create `app.yaml`:

```yaml
runtime: java17
env: flex

env_variables:
  SPRING_PROFILES_ACTIVE: prod
  GCP_CLOUD_SQL_CONNECTION_NAME: PROJECT_ID:REGION:INSTANCE_NAME
  GCP_DB_NAME: urlshortener
  GCP_DB_USERNAME: app-user

beta_settings:
  cloud_sql_instances: PROJECT_ID:REGION:INSTANCE_NAME

# Use Secret Manager for password
# secrets:
#   - name: db_password
#     value_from:
#       secret_manager_secret_version: projects/PROJECT_ID/secrets/db-password/versions/latest
```

Deploy:
```bash
gcloud app deploy
```

## API Endpoints

### Create Short URL

**POST** `/api/shortenUrl`

**Request Body:**
```json
{
  "shortenedUrl": "https://jonnoyip.com/abc123",
  "redirectedLink": "https://example.com/very/long/url"
}
```

**Response (201 Created):**
```json
{
  "shortenedUrl": "https://jonnoyip.com/abc123",
  "redirectedLink": "https://example.com/very/long/url",
  "createdAt": "2024-01-15T10:30:00",
  "message": "URL mapping created successfully"
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/shortenUrl \
  -H "Content-Type: application/json" \
  -d '{
    "shortenedUrl": "https://jonnoyip.com/test123",
    "redirectedLink": "https://www.google.com"
  }'
```

## Configuration

### Environment Variables

#### Development
- `DB_USERNAME` - Database username (default: `postgres`)
- `DB_PASSWORD` - Database password (default: `password`)
- `SPRING_PROFILES_ACTIVE` - Active profile (set to `dev`)

#### Production
- `GCP_CLOUD_SQL_CONNECTION_NAME` - Format: `PROJECT_ID:REGION:INSTANCE_NAME`
- `GCP_DB_NAME` - Database name (default: `urlshortener`)
- `GCP_DB_USERNAME` - Cloud SQL username
- `GCP_DB_PASSWORD` - Cloud SQL password
- `GCP_DB_URL` - Full JDBC URL (for TCP connections, optional)
- `SPRING_PROFILES_ACTIVE` - Active profile (set to `prod`)

### Application Properties

Configuration files:
- `application.properties` - Base configuration
- `application-dev.properties` - Development overrides
- `application-prod.properties` - Production overrides

Key settings:
- **Port**: `8080` (configurable via `server.port`)
- **Database**: Configured per profile
- **Connection Pool**: Optimized per environment
- **Logging**: Verbose in dev, minimal in prod

## Database Migrations

The application uses Flyway for database migrations. Migrations are located in:
```
src/main/resources/db/migration/
```

### Running Migrations

Migrations run automatically on application startup. To manually run:

```bash
# Using Flyway CLI (if installed)
flyway migrate -url=jdbc:postgresql://localhost:5432/urlshortener \
  -user=postgres -password=password \
  -locations=filesystem:src/main/resources/db/migration
```

### Creating New Migrations

1. Create a new SQL file: `V2__description.sql`
2. Place it in `src/main/resources/db/migration/`
3. Flyway will automatically apply it on next startup

## Security Best Practices

### Development
- Use strong passwords even in development
- Don't commit credentials to version control
- Use environment variables for sensitive data

### Production
- **Never commit passwords** to version control
- Use **Google Secret Manager** for sensitive data
- Enable **SSL/TLS** for database connections
- Use **IAM database authentication** when possible
- Restrict database access with firewall rules
- Regularly rotate passwords
- Use least-privilege database users

### Example: Using Secret Manager

```bash
# Create secret
echo -n "your-password" | gcloud secrets create db-password --data-file=-

# Grant access
gcloud secrets add-iam-policy-binding db-password \
  --member=serviceAccount:PROJECT_NUMBER-compute@developer.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor
```

## Troubleshooting

### Connection Issues

**Problem**: Cannot connect to database
- Check database is running
- Verify connection string
- Check firewall rules (for Cloud SQL)
- Verify credentials

**Problem**: Cloud SQL connection fails
- Verify `GCP_CLOUD_SQL_CONNECTION_NAME` format
- Check Cloud SQL instance is running
- Verify service account has Cloud SQL Client role
- For Unix socket: Ensure running on GCP infrastructure

### Migration Issues

**Problem**: Flyway migration fails
- Check database user has CREATE/ALTER permissions
- Verify migration files are in correct location
- Check for syntax errors in SQL files
- Review Flyway logs for specific errors

## Testing

```bash
# Run all tests
mvn test

# Run with specific profile
mvn test -Dspring.profiles.active=dev

# Run integration tests
mvn verify
```

## Monitoring

### Health Checks

- **Health endpoint**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **Info**: `http://localhost:8080/actuator/info`

### Logs

- **Development**: Console output with DEBUG level
- **Production**: Structured logging (configure log aggregation)

## Support

For issues or questions, please refer to:
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Google Cloud SQL Documentation: https://cloud.google.com/sql/docs
- Flyway Documentation: https://flywaydb.org/documentation
