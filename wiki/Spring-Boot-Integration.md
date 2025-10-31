# Spring Boot Integration

Aletheia integrates seamlessly with Spring Boot through auto-configuration.

## Automatic Configuration

Aletheia automatically configures itself when Spring Boot detects it on the classpath. No additional setup required!

## Automatic Secret Injection

Secrets are automatically injected into Spring beans using the `@Secret` annotation:

```java
import io.github.mwangiiharun.aletheia.annotations.Secret;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {
    @Secret("DATABASE_PASSWORD")
    private String dbPassword;
    
    @Secret(value = "API_KEY", required = false, defaultValue = "default")
    private String apiKey;
    
    // Secrets are injected automatically when Spring creates this bean
}
```

## Explicit Configuration

If you need more control, use `@EnableAletheia`:

```java
import io.github.mwangiiharun.aletheia.spring.EnableAletheia;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAletheia
public class AppConfig {
    // Your Spring configuration
}
```

## When to Use @EnableAletheia

Use `@EnableAletheia` when:

1. **Plain Spring Framework** - Not using Spring Boot's auto-configuration
2. **Conditional Activation** - Want to control when Aletheia is enabled
3. **Explicit Declaration** - Prefer explicit configuration
4. **Multiple Configuration Classes** - Want to enable in a specific config class

## Configuration Properties

You can configure Aletheia using Spring Boot's `application.properties`:

```properties
# Provider chain
aletheia.providers=VAULT,FILE,ENV

# Cache TTL
aletheia.cache.ttl.seconds=3600

# File provider
file.path=/path/to/secrets.json

# Vault configuration
vault.url=http://vault:8200
vault.token=${VAULT_TOKEN}
vault.path=secret/data/myapp

# AWS configuration
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}

# GCP configuration
GCP_PROJECT_ID=${GCP_PROJECT_ID}
GOOGLE_APPLICATION_CREDENTIALS=${GOOGLE_CREDENTIALS_PATH}
```

## Profile-Specific Configuration

Use Spring profiles to configure different providers for different environments:

**application-dev.properties**:
```properties
aletheia.providers=FILE,ENV
file.path=./secrets-dev.json
```

**application-prod.properties**:
```properties
aletheia.providers=VAULT,AWS,ENV
vault.url=https://vault.prod.example.com
AWS_REGION=us-east-1
```

## Accessing Aletheia Programmatically

You can also use Aletheia directly in your Spring code:

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    public void doSomething() {
        try {
            String secret = Aletheia.getSecret("MY_SECRET");
            // Use secret
        } catch (AletheiaException e) {
            // Handle error
        }
    }
}
```

## Testing with Spring Boot

In tests, you can use system properties to override secrets:

```java
@SpringBootTest
class MyServiceTest {
    @Test
    void testWithSecret() {
        System.setProperty("TEST_SECRET", "test-value");
        // Your test code
    }
}
```

Or use `@TestPropertySource`:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "aletheia.providers=ENV",
    "TEST_SECRET=test-value"
})
class MyServiceTest {
    // Your tests
}
```

## Best Practices

1. **Use Profiles** - Configure different providers for dev/staging/prod
2. **Environment Variables** - Use environment variables for sensitive config
3. **Fail Fast** - Use `required = true` (default) for critical secrets
4. **Default Values** - Use `defaultValue` for optional secrets with fallbacks

## Troubleshooting

- **Secrets not injected**: Check that your class is a Spring bean (`@Component`, `@Service`, etc.)
- **Provider not working**: Verify configuration properties are set correctly
- **Circular references**: Check for circular secret references

See [Common Issues](Common-Issues.md) for more troubleshooting tips.

