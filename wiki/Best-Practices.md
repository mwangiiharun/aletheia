# Best Practices

Follow these best practices to get the most out of Aletheia and maintain security.

## Security Best Practices

### 1. Never Commit Secrets

❌ **Don't do this**:
```java
@Secret("DATABASE_PASSWORD")
private String password = "hardcoded-secret"; // Never!
```

✅ **Do this**:
```java
@Secret("DATABASE_PASSWORD")
private String password; // Secret injected from provider
```

### 2. Use Provider Chain for Environment-Specific Secrets

```properties
# Development
aletheia.providers=FILE,ENV

# Production
aletheia.providers=VAULT,AWS,ENV
```

### 3. Rotate Secrets Regularly

Use cloud provider features to rotate secrets:
- AWS Secrets Manager automatic rotation
- GCP Secret Manager rotation policies
- Vault dynamic secrets

### 4. Use Least Privilege

Grant only the minimum permissions needed:
- AWS IAM policies scoped to specific secrets
- GCP IAM bindings for specific service accounts
- Vault policies with minimal paths

## Code Organization

### 1. Centralize Secret Configuration

Create a configuration class for all secrets:

```java
@Component
public class SecretsConfig {
    @Secret("DATABASE_PASSWORD")
    private String dbPassword;
    
    @Secret("API_KEY")
    private String apiKey;
    
    // Getters
    public String getDbPassword() { return dbPassword; }
    public String getApiKey() { return apiKey; }
}
```

### 2. Use Required vs Optional Secrets

```java
// Critical secret - fail if not found
@Secret("DATABASE_PASSWORD")
private String dbPassword;

// Optional secret - use default if not found
@Secret(value = "OPTIONAL_KEY", required = false, defaultValue = "default-value")
private String optionalKey;
```

### 3. Validate Secrets After Injection

```java
@PostConstruct
private void validateSecrets() {
    if (dbPassword == null || dbPassword.isEmpty()) {
        throw new IllegalStateException("Database password not configured");
    }
}
```

## Performance Optimization

### 1. Configure Appropriate Cache TTL

```properties
# Short TTL for frequently changing secrets
aletheia.cache.ttl.seconds=300

# Long TTL for stable secrets
aletheia.cache.ttl.seconds=86400
```

### 2. Order Providers by Cost/Speed

Put faster/cheaper providers first:

```properties
# ENV and FILE are fastest (local)
aletheia.providers=ENV,FILE,AWS,Vault
```

### 3. Use Environment Variables for Development

Environment variables are fastest and require no network calls:

```bash
export DEV_SECRET=value
```

## Configuration Management

### 1. Use Spring Profiles

```properties
# application-dev.properties
aletheia.providers=FILE,ENV
file.path=./secrets-dev.json

# application-prod.properties
aletheia.providers=VAULT,AWS
vault.url=https://vault.prod.example.com
```

### 2. Externalize Configuration

Use environment variables for sensitive configuration:

```bash
export VAULT_TOKEN=$(cat /run/secrets/vault-token)
export AWS_SECRET_ACCESS_KEY=$(cat /run/secrets/aws-key)
```

### 3. Document Secret Requirements

Maintain a list of required secrets:

```markdown
## Required Secrets

- `DATABASE_PASSWORD`: Database connection password
- `API_KEY`: External API key
- `JWT_SECRET`: JWT signing secret
```

## Error Handling

### 1. Handle Missing Secrets Gracefully

```java
@Secret(value = "API_KEY", required = false)
private String apiKey;

public void doSomething() {
    if (apiKey == null || apiKey.isEmpty()) {
        log.warn("API key not configured, using default behavior");
        return;
    }
    // Use apiKey
}
```

### 2. Log Secret Resolution (Without Values!)

```java
try {
    String secret = Aletheia.getSecret("MY_SECRET");
    log.info("Secret MY_SECRET resolved successfully");
} catch (SecretNotFoundException e) {
    log.error("Secret MY_SECRET not found in any provider", e);
}
```

**Never log secret values!**

### 3. Provide Helpful Error Messages

```java
try {
    Aletheia.injectSecrets(this);
} catch (SecretNotFoundException e) {
    throw new ConfigurationException(
        "Required secret " + e.getKey() + " not found. " +
        "Check your provider configuration.", e);
}
```

## Testing

### 1. Use System Properties in Tests

```java
@BeforeEach
void setup() {
    System.setProperty("TEST_SECRET", "test-value");
}

@AfterEach
void cleanup() {
    System.clearProperty("TEST_SECRET");
}
```

### 2. Mock Providers for Unit Tests

```java
@Test
void testWithMockProvider() {
    MockProvider mockProvider = new MockProvider();
    mockProvider.addSecret("TEST_KEY", "test-value");
    
    Aletheia.setProvidersForTesting(List.of(new CachedProvider(mockProvider, 3600)));
    // Your test
}
```

### 3. Test Provider Fallback

```java
@Test
void testProviderFallback() {
    // Setup: First provider fails, second succeeds
    // Verify: Secret is retrieved from second provider
}
```

## Monitoring and Observability

### 1. Monitor Secret Access

Log when secrets are accessed (but not their values):

```java
// This is built into Aletheia via SLF4J
// Configure logging level for com.aletheia
```

### 2. Alert on Secret Resolution Failures

Set up alerts when `SecretNotFoundException` occurs in production.

### 3. Track Secret Usage

Document which services use which secrets for better secret rotation planning.

## Migration

### 1. Gradual Migration

Migrate secrets gradually:

1. Start with non-critical secrets
2. Test thoroughly in dev/staging
3. Migrate production secrets during low-traffic periods

### 2. Maintain Backward Compatibility

Keep old configuration methods working during migration:

```properties
# Old way (still works)
DATABASE_PASSWORD=old-value

# New way (preferred)
aletheia.providers=VAULT
```

---

**Remember**: Security is not optional. Always follow security best practices when handling secrets!

