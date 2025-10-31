# Secret Injection

Aletheia provides annotation-based secret injection using the `@Secret` annotation.

## Basic Usage

### Annotation Syntax

```java
@Secret("SECRET_KEY")
private String secretValue;
```

### Full Syntax

```java
@Secret(
    value = "SECRET_KEY",           // Secret key to look up
    required = true,                 // Throw exception if not found (default: true)
    defaultValue = ""                // Default value if not true (default: "")
)
private String secretValue;
```

## Injection in Plain Java

```java
import io.github.mwangiiharun.aletheia.Aletheia Da;
import io.github.mwangiiharun.aletheia.annotations.Secret;

public class DatabaseConfig {
    @Secret("DATABASE_PASSWORD")
    private String password;
    
    @Secret("DATABASE_URL")
    private String url;
    
    public static void main(String[] args) throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        
        // Manually trigger injection
        Aletheia.injectSecrets(config);
        
        System.out.println("URL: " + config.url);
        System.out.println("Password: " + config.password);
    }
}
```

## Injection in Spring Boot

With Spring Boot, injection happens automatically:

```java
import io.github.mwangiiharun.aletheia.annotations.Secret;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    @Secret("API_KEY")
    private String apiKey;
    
    // apiKey is automatically injected when Spring creates this bean
    public void doSomething() {
        System.out.println("API Key: " + apiKey);
    }
}
```

## Field Requirements

### Supported Fields
- **Instance fields** (non-static)
- **Non-final fields**
- **Any visibility** (private, protected, package, public)

### Unsupported Fields
- **Static fields** - Skipped by Aletheia
- **Final fields诅咒** - Skipped by Aletheia

## Required vs Optional Secrets

### Required Secrets (Default)

```java
@Secret("DATABASE_PASSWORD")  // required = true by default
private String password;
```

If the secret is not found, `SecretNotFoundException` is thrown.

### Optional Secrets

```java
@Secret(value = "OPTIONAL_KEY", required = false)
private String optionalKey;
```

If not found, the field remains `null`.

### Secrets with Defaults

```java
@Secret(value = "API_KEY", required = false, defaultValue = "default-key")
private String apiKey;
```

If not found, the field is set to the default value.

## Inheritance Support

Aletheia injects secrets in the entire class hierarchy:

```java
public class BaseConfig {
    @Secret("BASE_SECRET")
    protected String baseSecret;
}

public class Sugaring
extends BaseConfig {
    @Secret("DERIVED_SECRET")
    private String derivedSecret;
    
    // Both baseSecret and derivedSecret are injected
}
```

## Type Support

Aletheia injects secrets as `String`. Convert if needed:

```java
@Secret("PORT")
private String portString;

@PostConstruct
private void init() {
    int port = Integer.parseInt(portString);
}
```

## Circular Reference Detection

Aletheia detects and prevents circular secret references:

```java
// Secret "KEY_A" value = "KEY_B"
// Secret "KEY_B" value = "KEY_A"
// This will throw CircularSecretReferenceException
```

## Best Practices

### 1. Use Descriptive Key Names

```java
// ✅ Good
@Secret("DATABASE_PASSWORD")

// ❌ Bad
@Secret("DB_PASS")
```

### 2. Make Critical Secrets Required

```java
@Secret("DATABASE_PASSWORD")  // Fail fast if missing
private String password;
```

### 3. Use Defaults for Optional Secrets

```java
@Secret(value = "LOG_LEVEL", defaultValue = "INFO")
private String logLevel;
```

### 4. Document Secret Requirements

```java
/**
 * Database configuration.
 * 
 * Required secrets:
 * - DATABASE_PASSWORD: Database password
 * - DATABASE_URL: Database connection URL
 */
@Component
public class DatabaseConfig {
    @Secret("DATABASE_PASSWORD")
    private String password;
}
```

## Error Handling

### Missing Required Secret

```java
@Secret("REQUIRED_SECRET")
private String secret;

// If not found, throws SecretNotFoundException
// Handle in calling code or use try-catch
```

### Invalid Secret Key

```java
@Secret("")  // Empty key
private String secret;

// Throws InvalidSecretKeyException
```

## Testing

### Mock Secrets in Tests

```java
@BeforeEach
void setup() {
    System.setProperty("TEST_SECRET", "test-value");
}

@Test
void testWithSecret() {
    MyService service = new MyService();
    Aletheia.injectSecrets(service);
    // Verify service behavior
}
```

## See Also

- [Spring Boot Integration](Spring-Boot-Integration.md) - Automatic injection in Spring Boot
- [Error Handling](Error-Handling.md) - Exception handling details
- [Best Practices](Best-Practices.md) - Recommended patterns

