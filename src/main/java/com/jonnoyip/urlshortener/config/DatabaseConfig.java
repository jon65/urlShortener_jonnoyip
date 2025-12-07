package com.jonnoyip.urlshortener.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Database configuration for Google Cloud SQL
 * 
 * This configuration is automatically applied when using Cloud SQL Socket Factory.
 * The connection is configured via application-prod.properties
 * 
 * For Unix socket connections (GCP App Engine, Cloud Run, GCE):
 * - Set GCP_CLOUD_SQL_CONNECTION_NAME environment variable
 * - Format: PROJECT_ID:REGION:INSTANCE_NAME
 * - Example: my-project:us-central1:my-instance
 * 
 * For TCP connections (Cloud SQL Proxy or direct):
 * - Set GCP_DB_URL, GCP_DB_USERNAME, GCP_DB_PASSWORD
 * - Or use Cloud SQL Proxy: jdbc:postgresql://localhost:5432/dbname
 */
@Configuration
public class DatabaseConfig {
    
    // Configuration is handled via application.properties
    // Cloud SQL Socket Factory is automatically configured when:
    // 1. The dependency is present (postgres-socket-factory)
    // 2. The JDBC URL includes socketFactory parameter
    // 3. cloudSqlInstance parameter is provided
    
    // No additional Java configuration needed for basic setup
    // HikariCP connection pool is auto-configured by Spring Boot
}

