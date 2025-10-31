# Error Handling

Understanding Aletheia's exception hierarchy and how to handle errors.

## Exception Hierarchy

```
AletheiaException (base)
├── AletheiaInitializationException
├── ConfigLoadException
├── InvalidSecretKeyException
├── ProviderException
│   └── SecretNotFoundException
├── CircularSecretReferenceException
└── SecretInjectionException
```

## Exception Types

### AletheiaException

Base exception for all Aletheia-related errors.

```java
try {
    Aletheia.getSecret("MY_SECRET");
} catch (AletheiaException e) {
    // Handle any Aletheia error
}
```

### SecretNotFoundException

Thrown when a secret cannot be found in any configured provider.

```java
try {
    String secret = Aletheia.getSecret("NON_EXISTENT_SECRET");
} catch (SecretNotFoundException e) {
    System.err.println("Secret not found: " + e.getKey());
    // Handle missing secret
}
```

**When it occurs:**
- Secret key doesn't exist in any provider
- All providers fail to retrieve the secret
- Provider chain exhausted

**How to handle:**
- Use optional secrets (`required = false`)
- Provide default values
- Fail fast for critical secrets

### InvalidSecretKeyException

Thrown when an invalid secret key is provided.

```java
try {
    Aletheia.getSecret("");  // Empty key
} catch (InvalidSecretKeyException e) {
    System.err.println("Invalid key: " + e.getMessage());
}
```

**When it occurs:**
- Secret key is `null`
- Secret key is empty or blank

**How to handle:**
- Validate keys before calling Aletheia
- Use constants for secret keys

### CircularSecretReferenceException

Thrown when a circular reference is detected in secret resolution.

```java
// Secret "KEY_A" = "KEY_B"
// Secret "KEY_B" = "KEY_A"
try {
    Aletheia.getSecret("KEY_A");
} catch (CircularSecretReferenceException e) {
    System.err.println("Circular reference: " + e.getMessage());
}
```

**When it occurs:**
- Secret value references another secret
- That secret references back to the first

**How to handle:**
- Restructure secret dependencies
- Use direct values instead of references
- Review secret configuration

### ProviderException

Base exception for provider-related errors. Specific provider errors wrap this.

```java
try {
    Aletheia.getSecret("MY_SECRET");
} catch (ProviderException e) {
    System.err.println("Provider error: " + e.getProviderName());
    System.err.println("Cause: " + e.getCause());
}
```

**When it occurs:**
- Provider fails to initialize
- Provider network error
- Provider authentication failure

### SecretInjectionException

Thrown when secret injection fails (reflection error).

```java
@Secret("MY_SECRET")
private final String secret;  // Final field

try {
    Aletheia.injectSecrets(this);
} catch (SecretInjectionException e) {
    System.err.println("Failed to inject: " + e.getFieldName());
}
```

**When it occurs:**
- Field is final or static (should be skipped, but edge cases exist)
- Reflection access denied

**How to handle:**
- Ensure fields are not final/static
- Check field visibility

### ConfigLoadException

Thrown when configuration cannot be loaded.

```java
try {
    Aletheia.reinitializeProviders();
} catch (AletheiaInitializationException e) {
    if (e.getCause() instanceof ConfigLoadException) {
        // Handle config error
    }
}
```

### AletheiaInitializationException

Thrown when Aletheia fails to initialize.

```java
// Usually thrown during static initialization
// Check logs for details
```

## Error Handling Strategies

### Strategy 1: Fail Fast

For critical secrets, let the exception propagate:

```java
@Secret("DATABASE_PASSWORD")  // required = true
private String password;

// If missing, application fails to start
// This is desirable for critical secrets
```

### Strategy 2: Use Optional Secrets

For non-critical secrets:

```java
@Secret(value = "FEATURE_FLAG", required = false, defaultValue = "false")
private String featureFlag;

// Never throws exception
```

### Strategy 3: Try-Catch with Fallback

```java
String secret;
try {
    secret = Aletheia.getSecret("MY_SECRET");
} catch (SecretNotFoundException e) {
    log.warn("Secret not found, using default");
    secret = "default-value";
}
```

### Strategy 4: Validation After Injection

```java
@PostConstruct
private void validateSecrets() {
    if (password == null || password.isEmpty()) {
        throw new IllegalStateException("Password not configured");
    }
}
```

## Logging

Aletheia uses SLF4J for logging. Configure logging levels:

```properties
# Log all Aletheia debug messages
logging.level.io.github.mwangiiharun.aletheia=DEBUG

# Log only errors
logging.level.io.github.mwangiiharun.aletheia=ERROR
```

### Log Messages

- **INFO**: Provider initialization, configuration loaded
- **WARN**: Unknown provider types, invalid configuration, cache reset failures
- **DEBUG**: Secret resolution attempts, provider queries
- **ERROR**: Provider failures, injection errors

## Best Practices

### 1. Handle Specific Exceptions

```java
try {
    String secret = Aletheia.getSecret("MY_SECRET");
} catch (SecretNotFoundException e) {
    // Handle missing secret
} catch (InvalidSecretKeyException e) {
    // Handle invalid key
} catch (AletheiaException e) {
    // Handle other errors
}
```

### 2. Never Log Secret Values

```java
// ✅ Good
log.info("Secret MY_SECRET retrieved successfully");

// ❌ Bad - Never do this!
log.info("Secret MY_SECRET = " + secret);
```

### 3. Provide Context in Error Messages

```java
catch (SecretNotFoundException e) {
    throw new ConfigurationException(
        "Required secret '" + e.getKey() + "' not found. " +
        "Check your provider configuration.", e);
}
```

### 4. Use Structured Error Handling

```java
public class SecretResolver {
    public Optional<String> resolveSecret(String key) {
        try {
            return Optional.of(Aletheia.getSecret(key));
        } catch (SecretNotFoundException e) {
            return Optional.empty();
        } catch (AletheiaException e) {
            log.error("Failed to resolve secret: " + key, e);
            throw new SecretResolutionException("Failed to resolve: " + key, e);
        }
    }
}
```

## Common Error Scenarios

### Scenario 1: Secret Not Configured

**Error**: `SecretNotFoundException`

**Solution**: 
- Check secret exists in provider
- Verify provider configuration
- Use `required = false` if optional

### Scenario 2: Provider Unavailable

**Error**: `ProviderException` with network/authentication cause

**Solution**:
- Check network connectivity
- Verify credentials
- Check provider status
- Use fallback providers

### Scenario 3: Invalid Configuration

**Error**: `AletheiaInitializationException`

**Solution**:
- Validate `aletheia.properties`
- Check required provider configuration
- Review logs for specific errors

## See Also

- [Common Issues](Common-Issues.md) - Solutions to common problems
- [Debugging](Debugging.md) - Tips for debugging
- [Best Practices](Best-Practices.md) - Error handling best practices

