```
    ___    __            __  __         __    __
   /   |  / /___  ____ _/ /_/ /_  ___  / /_  / /__
  / /| | / / __ \/ __ `/ __/ __ \/ _ \/ __ \/ //_/
 / ___ |/ / /_/ / /_/ / /_/ / / /  __/ /_/ / ,<
/_/  |_/_/\____/\__,_/\__/_/ /_/\___/\____/_/|_|
```

# Aletheia

[![Maven Central](https://img.shields.io/maven-central/v/com.aletheia/aletheia.svg)](https://search.maven.org/artifact/com.aletheia/aletheia)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

> **ἀλήθεια** (Greek: "truth") - Bringing truth and transparency to secret management

**Aletheia** is a lightweight, framework-agnostic secret management library for Java 17+ that simplifies secure configuration management across different environments. It provides annotation-based secret injection with support for multiple providers, automatic fallback chains, and seamless integration with Spring Boot and Quarkus.

## Features

✨ **Multi-Provider Support** - Fetch secrets from Environment Variables, Files, AWS Secrets Manager, Google Cloud Secret Manager, or HashiCorp Vault  
🔄 **Automatic Fallback** - Configure a chain of providers with automatic fallback when secrets aren't found  
💉 **Annotation-Based Injection** - Use `@Secret` annotations to automatically inject secrets into your classes  
🏗️ **Framework Agnostic** - Works standalone or integrates seamlessly with Spring Boot and Quarkus  
🛡️ **Thread-Safe** - Built with thread-safety in mind for concurrent environments  
⚡ **Caching** - Built-in TTL-based caching to reduce provider calls  
🔒 **Type-Safe** - Leverage Java's type system for better security  

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.aletheia</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.aletheia:aletheia:0.1.0-SNAPSHOT'
```

## Quick Start

### Basic Usage

```java
import com.aletheia.Aletheia;
import com.aletheia.annotations.Secret;

public class AppConfig {
    @Secret("DATABASE_PASSWORD")
    private String dbPassword;
    
    @Secret(value = "API_KEY", required = false, defaultValue = "default-key")
    private String apiKey;
    
    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        
        // Inject secrets from configured providers
        Aletheia.injectSecrets(config);
        
        System.out.println("DB Password: " + config.dbPassword);
        System.out.println("API Key: " + config.apiKey);
    }
}
```

### Direct Secret Retrieval

```java
import com.aletheia.Aletheia;
import com.aletheia.exceptions.AletheiaException;

public class Example {
    public static void main(String[] args) {
        try {
            String secret = Aletheia.getSecret("MY_SECRET_KEY");
            System.out.println("Secret: " + secret);
        } catch (AletheiaException e) {
            System.err.println("Failed to retrieve secret: " + e.getMessage());
        }
    }
}
```

## Configuration

Aletheia can be configured via:

1. **System Properties**
2. **Environment Variables**
3. **`aletheia.properties` file** (in classpath)

### Provider Configuration

Configure the provider chain using the `aletheia.providers` property:

```properties
# Provider order (comma-separated, highest priority first)
aletheia.providers=VAULT,AWS,GCP,FILE,ENV
```

Available providers:
- `ENV` - Environment variables and system properties
- `FILE` - JSON file-based secrets
- `AWS` - AWS Secrets Manager
- `GCP` - Google Cloud Secret Manager
- `VAULT` - HashiCorp Vault

### Cache Configuration

```properties
# Cache TTL in seconds (default: 3600)
aletheia.cache.ttl.seconds=1800
```

## Provider-Specific Configuration

### 1. Environment Variables Provider (`ENV`)

No additional configuration needed. This provider reads from:
- System environment variables
- JVM system properties

**Priority**: System properties take precedence over environment variables.

```bash
export DATABASE_URL=postgresql://localhost:5432/mydb
```

Or via system properties:
```java
System.setProperty("DATABASE_URL", "postgresql://localhost:5432/mydb");
```

### 2. File Provider (`FILE`)

Store secrets in a JSON file:

```json
{
  "DATABASE_PASSWORD": "secret123",
  "API_KEY": "abc-xyz-789",
  "JWT_SECRET": "my-jwt-secret"
}
```

**Configuration:**

```properties
# Path to secrets JSON file
file.path=/path/to/secrets.json
```

Or via system property:
```bash
-Dfile.path=/path/to/secrets.json
```

### 3. AWS Secrets Manager (`AWS`)

**Configuration Options:**

**Option 1: Environment Variables**
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

**Option 2: System Properties**
```bash
-DAWS_REGION=us-east-1
-DAWS_ACCESS_KEY_ID=your-access-key
-DAWS_SECRET_ACCESS_KEY=your-secret-key
```

**Option 3: `aletheia.properties`**
```properties
aws.region=us-east-1
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
```

**Option 4: Spring Boot `application.properties`**
```properties
spring.cloud.aws.region.static=us-east-1
spring.cloud.aws.credentials.access-key=your-access-key
spring.cloud.aws.credentials.secret-key=your-secret-key
```

**Option 5: Quarkus `application.properties`**
```properties
quarkus.aws.region=us-east-1
quarkus.aws.credentials.access-key-id=your-access-key
quarkus.aws.credentials.secret-access-key=your-secret-key
```

**Usage:**
```java
// Retrieve secret by its name in AWS Secrets Manager
String secret = Aletheia.getSecret("my-secret-name");
```

### 4. Google Cloud Secret Manager (`GCP`)

**Configuration Options:**

**Option 1: Environment Variables**
```bash
export GCP_PROJECT_ID=my-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

**Option 2: `aletheia.properties`**
```properties
gcp.project.id=my-project-id
gcp.credentials.file=/path/to/service-account.json
```

**Option 3: Spring Boot `application.properties`**
```properties
spring.cloud.gcp.project-id=my-project-id
spring.cloud.gcp.credentials.location=/path/to/service-account.json
```

**Option 4: Quarkus `application.properties`**
```properties
quarkus.google.cloud.project-id=my-project-id
quarkus.google.cloud.credentials.location=/path/to/service-account.json
```

**Usage:**
```java
// Retrieve secret by its name in GCP Secret Manager
String secret = Aletheia.getSecret("my-secret-name");
```

### 5. HashiCorp Vault (`VAULT`)

**Configuration Options:**

**Option 1: Environment Variables**
```bash
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=your-vault-token
```

**Option 2: System Properties**
```bash
-Dvault.url=http://localhost:8200
-Dvault.token=your-vault-token
```

**Option 3: `aletheia.properties`**
```properties
vault.url=http://localhost:8200
vault.token=your-vault-token
```

**Option 4: Spring Boot `application.properties`**
```properties
spring.cloud.vault.uri=http://localhost:8200
spring.cloud.vault.token=your-vault-token
```

**Option 5: Quarkus `application.properties`**
```properties
quarkus.vault.url=http://localhost:8200
quarkus.vault.authentication.client-token=your-vault-token
```

**Usage:**

For Vault KV v2 secrets:
```java
// Path format: secret/data/path/to/secret
String secret = Aletheia.getSecret("secret/data/myapp/database/password");
```

For Vault KV v1 secrets:
```java
String secret = Aletheia.getSecret("secret/myapp/database/password");
```

## Annotation-Based Injection

The `@Secret` annotation provides declarative secret injection:

```java
import com.aletheia.annotations.Secret;

public class DatabaseConfig {
    @Secret("DB_HOST")
    private String host;
    
    @Secret("DB_PORT")
    private String port;
    
    @Secret(value = "DB_PASSWORD", required = true)
    private String password;
    
    @Secret(value = "DB_SSL", required = false, defaultValue = "false")
    private String sslEnabled;
}
```

### Annotation Parameters

- **`value`** (required): The secret key to retrieve
- **`required`** (default: `true`): Whether the secret is required. If `true` and secret is not found, throws `SecretNotFoundException`
- **`defaultValue`** (default: `""`): Default value if secret is not found (only used if `required=false`)

### Injection Example

```java
public class Application {
    public static void main(String[] args) throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        
        // Inject all secrets marked with @Secret
        Aletheia.injectSecrets(config);
        
        System.out.println("Host: " + config.host);
        System.out.println("Port: " + config.port);
        System.out.println("SSL: " + config.sslEnabled);
    }
}
```

### Inheritance Support

Aletheia supports injection into inherited fields:

```java
public class BaseConfig {
    @Secret("COMMON_API_KEY")
    protected String apiKey;
}

public class AppConfig extends BaseConfig {
    @Secret("APP_SPECIFIC_SECRET")
    private String appSecret;
}

// Both fields will be injected
AppConfig config = new AppConfig();
Aletheia.injectSecrets(config);
```

## Spring Boot Integration

Aletheia provides automatic Spring Boot integration via auto-configuration with two approaches: **automatic** and **explicit** via `@EnableAletheia`.

### Automatic Configuration (Spring Boot Only)

When using Spring Boot, Aletheia automatically configures itself via Spring Boot's auto-configuration mechanism. No additional setup needed!

Aletheia registers a `BeanPostProcessor` that automatically injects secrets into all Spring-managed beans before initialization.

### Explicit Configuration with @EnableAletheia

For more control or when using plain Spring (without Spring Boot), use the `@EnableAletheia` annotation:

```java
import com.aletheia.spring.EnableAletheia;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAletheia
public class AppConfig {
    // Your Spring configuration
}
```

**When to use `@EnableAletheia`:**

1. **Plain Spring Framework** - When not using Spring Boot's auto-configuration
2. **Conditional Activation** - When you want to control when Aletheia is enabled
3. **Explicit Declaration** - When you prefer explicit configuration over auto-configuration
4. **Multiple Configuration Classes** - When you want to enable Aletheia in a specific configuration class

**What `@EnableAletheia` does:**

- Imports `AletheiaAutoConfiguration` which registers:
  - A `BeanPostProcessor` that injects `@Secret` annotated fields in all Spring beans
  - Initializes Aletheia providers during Spring context startup
  - Ensures secrets are injected before bean initialization

**Example with Spring Framework (non-Boot):**

```java
import com.aletheia.spring.EnableAletheia;
import com.aletheia.annotations.Secret;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan("com.yourcompany.app")
@EnableAletheia  // Explicitly enable Aletheia
public class ApplicationConfiguration {
    // Configuration
}

@Component
public class DatabaseService {
    @Secret("DATABASE_URL")
    private String databaseUrl;
    
    // Secret is automatically injected via BeanPostProcessor
}
```

**Example with Conditional Activation:**

```java
import com.aletheia.spring.EnableAletheia;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")  // Enable Aletheia except in test profile
@EnableAletheia
public class ProductionConfig {
    // Production-specific configuration
}
```

### Usage in Spring Beans

```java
import com.aletheia.annotations.Secret;
import org.springframework.stereotype.Component;

@Component
public class DatabaseService {
    @Secret("DATABASE_URL")
    private String databaseUrl;
    
    @Secret("DATABASE_USER")
    private String databaseUser;
    
    @Secret("DATABASE_PASSWORD")
    private String databasePassword;
    
    // Secrets are automatically injected when bean is created
    public void connect() {
        // Use injected secrets
        System.out.println("Connecting to: " + databaseUrl);
    }
}
```

### Manual Injection in Spring

If you need manual control:

```java
import com.aletheia.Aletheia;
import org.springframework.beans.factory.InitializingBean;

@Component
public class CustomService implements InitializingBean {
    @Secret("MY_SECRET")
    private String secret;
    
    @Override
    public void afterPropertiesSet() {
        // Manual injection after Spring initialization
        Aletheia.injectSecrets(this);
    }
}
```

### Spring Boot Configuration

Configure providers in `application.properties`:

```properties
# Aletheia configuration
aletheia.providers=AWS,FILE,ENV
aletheia.cache.ttl.seconds=3600

# AWS configuration (if using AWS provider)
spring.cloud.aws.region.static=us-east-1

# File provider configuration
file.path=./secrets.json
```

## Provider Chain and Fallback

Aletheia supports multiple providers in a fallback chain:

```properties
aletheia.providers=VAULT,AWS,FILE,ENV
```

When retrieving a secret:
1. Aletheia queries **Vault** first
2. If not found, queries **AWS Secrets Manager**
3. If not found, queries **File provider**
4. If not found, queries **Environment variables**
5. If still not found, throws `SecretNotFoundException` (unless `required=false` with a default value)

**Example:**

```java
// This secret will be retrieved from the first provider that has it
String secret = Aletheia.getSecret("DATABASE_PASSWORD");
```

## Error Handling

Aletheia provides specific exception types for better error handling:

```java
import com.aletheia.exceptions.*;
import com.aletheia.Aletheia;

try {
    String secret = Aletheia.getSecret("MY_SECRET");
} catch (SecretNotFoundException e) {
    // Secret not found in any provider
    System.err.println("Secret not found: " + e.getMessage());
} catch (ProviderException e) {
    // Provider-specific error (network, auth, etc.)
    System.err.println("Provider error: " + e.getMessage());
} catch (CircularSecretReferenceException e) {
    // Circular reference detected (rare)
    System.err.println("Circular reference: " + e.getMessage());
} catch (AletheiaException e) {
    // General Aletheia error
    System.err.println("Error: " + e.getMessage());
}
```

## Advanced Usage

### Custom Provider Order

Configure different provider orders per environment:

**Development (`application-dev.properties`):**
```properties
aletheia.providers=FILE,ENV
file.path=./dev-secrets.json
```

**Production (`application-prod.properties`):**
```properties
aletheia.providers=VAULT,AWS,ENV
```

### Programmatic Provider Configuration

For testing or advanced scenarios:

```java
import com.aletheia.Aletheia;
import com.aletheia.SecretProvider;
import com.aletheia.providers.EnvProvider;
import com.aletheia.providers.FileProvider;

// Reinitialize with custom providers
Aletheia.reinitializeProviders();

// Or use reflection to set custom providers for testing
// (Note: This is primarily for testing scenarios)
```

### Caching Behavior

Secrets are cached per provider with TTL:

```properties
# Cache for 30 minutes
aletheia.cache.ttl.seconds=1800
```

Cache behavior:
- Each provider maintains its own cache
- Cache is per-key (different keys cached independently)
- TTL is global across all providers
- Cache is cleared when providers are reinitialized

### Thread Safety

Aletheia is thread-safe and can be used in concurrent environments:

```java
// Safe to call from multiple threads
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> 
    Aletheia.getSecret("SECRET_1"));
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> 
    Aletheia.getSecret("SECRET_2"));
```

## Best Practices

### 1. Provider Order

Order providers by security and availability:
```
Production: VAULT > AWS > ENV
Development: FILE > ENV
Testing: ENV only
```

### 2. Secret Naming

Use consistent naming conventions:
```java
@Secret("DATABASE_PASSWORD")  // ✅ Clear and descriptive
@Secret("db_pwd")              // ❌ Unclear
```

### 3. Default Values

Use default values for non-critical configuration:
```java
@Secret(value = "LOG_LEVEL", required = false, defaultValue = "INFO")
private String logLevel;
```

### 4. Error Handling

Always handle `AletheiaException`:
```java
try {
    Aletheia.injectSecrets(config);
} catch (SecretNotFoundException e) {
    logger.error("Required secret missing", e);
    throw new ConfigurationException("Invalid configuration", e);
}
```

### 5. Testing

For tests, use environment variables or file provider:
```java
@BeforeEach
void setup() {
    System.setProperty("TEST_SECRET", "test-value");
    Aletheia.reinitializeProviders();
}
```

## Complete Example

Here's a complete example showing all features:

```java
import com.aletheia.Aletheia;
import com.aletheia.annotations.Secret;
import com.aletheia.exceptions.AletheiaException;

public class CompleteExample {
    
    public static class AppConfiguration {
        @Secret("DATABASE_URL")
        private String databaseUrl;
        
        @Secret("DATABASE_USERNAME")
        private String databaseUsername;
        
        @Secret("DATABASE_PASSWORD")
        private String databasePassword;
        
        @Secret(value = "CACHE_TTL", required = false, defaultValue = "3600")
        private String cacheTtl;
        
        // Getters
        public String getDatabaseUrl() { return databaseUrl; }
        public String getDatabaseUsername() { return databaseUsername; }
        public String getDatabasePassword() { return databasePassword; }
        public String getCacheTtl() { return cacheTtl; }
    }
    
    public static void main(String[] args) {
        try {
            // Direct secret retrieval
            String apiKey = Aletheia.getSecret("API_KEY");
            System.out.println("API Key retrieved: " + apiKey);
            
            // Annotation-based injection
            AppConfiguration config = new AppConfiguration();
            Aletheia.injectSecrets(config);
            
            System.out.println("Database URL: " + config.getDatabaseUrl());
            System.out.println("Database User: " + config.getDatabaseUsername());
            System.out.println("Cache TTL: " + config.getCacheTtl());
            
        } catch (AletheiaException e) {
            System.err.println("Error retrieving secrets: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## Troubleshooting

### Secret Not Found

**Problem:** `SecretNotFoundException` is thrown

**Solutions:**
1. Verify the secret key name matches exactly (case-sensitive)
2. Check provider configuration and order
3. Verify the secret exists in at least one provider
4. Use `required=false` with a `defaultValue` if the secret is optional

### Provider Not Active

**Problem:** Cloud provider (AWS/GCP/Vault) is not active

**Solutions:**
1. Verify credentials/configuration are correct
2. Check network connectivity
3. Verify required environment variables/system properties are set
4. Check provider logs for specific error messages

### Configuration Not Detected

**Problem:** Provider doesn't detect Spring Boot/Quarkus configuration

**Solutions:**
1. Ensure `application.properties` is in the classpath
2. Verify property names match exactly (case-sensitive)
3. Check that `user.dir` system property points to the correct directory
4. Use explicit environment variables or system properties as fallback

## Migration Guide

### From Environment Variables Only

```java
// Before
String dbUrl = System.getenv("DATABASE_URL");

// After
@Secret("DATABASE_URL")
private String dbUrl;
```

### From Spring Cloud Config

Aletheia can complement or replace Spring Cloud Config for secret management:

```properties
# Spring Cloud Config
spring.cloud.config.server.git.uri=...

# Aletheia (for secrets)
aletheia.providers=VAULT,AWS,FILE,ENV
```

## Contributing

Contributions are welcome! Please see our contributing guidelines.

## License

Licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/aletheia/aletheia/issues)
- **Documentation**: [GitHub Wiki](https://github.com/aletheia/aletheia/wiki)

## Authors

- **Mwangii K.** - *Lead Developer*

---

**Aletheia** - *Truth in Secrets* 🔐

