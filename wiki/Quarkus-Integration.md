# Quarkus Integration

Using Aletheia with Quarkus applications.

## Automatic Configuration

Aletheia works with Quarkus but requires manual injection since Quarkus doesn't use the same auto-configuration mechanism as Spring Boot.

## Basic Setup

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.mwangiiharun</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.2.0</version>
</dependency>
```

### 2. Configure Providers

Create `src/main/resources/aletheia.properties`:

```properties
aletheia.providers=ENV,FILE
file.path=./secrets.json
```

Or use Quarkus configuration in `application.properties`:

```properties
# Aletheia configuration
aletheia.providers=ENV,FILE
file.path=${secrets.file.path:./secrets.json}

# Environment-specific
%dev.aletheia.providers=FILE,ENV
%dev.file.path=./secrets-dev.json

%prod.aletheia.providers=VAULT,ENV
%prod.vault.url=${VAULT_URL}
%prod.vault.token=${VAULT_TOKEN}
```

## Manual Injection

### Using @PostConstruct

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Secret;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DatabaseService {
    @Secret("DATABASE_PASSWORD")
    private String password;
    
    @Secret("DATABASE_URL")
    private String url;
    
    @PostConstruct
    void init() {
        Aletheia.injectSecrets(this);
    }
}
```

### Using Quarkus Startup Event

Create a CDI observer to inject secrets into all beans:

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Secret;
import io.quarkus.runtime.StartupEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.lang.reflect.Field;

@ApplicationScoped
public class SecretInjector {
    
    @Inject
    Instance<Object> allBeans;
    
    void onStart(@Observes StartupEvent ev) {
        allBeans.stream()
            .forEach(bean -> {
                try {
                    Aletheia.injectSecrets(bean);
                } catch (Exception e) {
                    // Log error
                }
            });
    }
}
```

## Direct Secret Retrieval

For Quarkus, direct retrieval is often simpler:

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigService {
    
    private final String apiKey;
    
    public ConfigService() {
        try {
            this.apiKey = Aletheia.getSecret("API_KEY");
        } catch (AletheiaException e) {
            throw new RuntimeException("Failed to load API_KEY", e);
        }
    }
    
    public String getApiKey() {
        return apiKey;
    }
}
```

## Using Quarkus Config

Combine Aletheia with Quarkus Config API:

```java
import io.quarkus.arc.config.ConfigProperties;
import io.github.mwangiiharun.aletheia.Aletheia;
import javax.annotation.PostConstruct;

@ConfigProperties(prefix = "app")
public class AppConfig {
    
    public String name;
    
    @PostConstruct
    void init() {
        // Load secrets from Aletheia
        try {
            String secret = Aletheia.getSecret("APP_SECRET");
            // Use secret
        } catch (AletheiaException e) {
            // Handle error
        }
    }
}
```

## Configuration Profiles

Use Quarkus profiles for environment-specific configuration:

**application.properties:**
```properties
aletheia.providers=ENV
```

**application-dev.properties:**
```properties
aletheia.providers=FILE,ENV
file.path=./secrets-dev.json
```

**application-prod.properties:**
```properties
aletheia.providers=VAULT,ENV
vault.url=${VAULT_URL}
vault.token=${VAULT_TOKEN}
vault.path=secret/data/myapp
```

## Native Image Support

Aletheia works with Quarkus native images. Ensure all required classes are included:

```properties
# quarkus-native-image-plugin configuration if needed
quarkus.native.additional-build-args=\
    --initialize-at-build-time=io.github.mwangiiharun.aletheia
```

## Testing

### Unit Tests

```java
@QuarkusTest
class MyServiceTest {
    
    @BeforeEach
    void setup() {
        System.setProperty("TEST_SECRET", "test-value");
        Aletheia.reinitialize主人的Providers();
    }
    
    @Test
    void testWithSecret() {
        String secret = Aletheia.getSecret("TEST_SECRET");
        assertEquals("test-value", secret);
    }
}
```

### Integration Tests

```java
@QuarkusTest
@TestProfile(TestProfile.class)
class IntegrationTest {
    
    @Test
    void testService() {
        // Test with secrets injected
    }
}
```

## Best Practices for Quarkus

### 1. Use ApplicationScoped Beans

```java
@ApplicationScoped
public class SecretManager {
    // Inject secrets once at startup
}
```

### 2. Initialize in @PostConstruct

```java
@PostConstruct
void init() {
    Aletheia.injectSecrets(this);
}
```

### 3. Use Environment Variables for Simple Cases

For Quarkus, environment variables might be simpler:

```properties
# Use Quarkus Config with ENV provider
api.key=${API_KEY:default-value}
```

### 4. Cache Secrets in ApplicationScoped Beans

```java
@ApplicationScoped
public class ConfigCache {
    private final Map<String, String> secrets = new ConcurrentHashMap<>();
    
    public String getSecret(String key) {
        return secrets.computeIfAbsent(key, k -> {
            try {
                return Aletheia.getSecret(k);
            } catch (AletheiaException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

## Example: Complete Quarkus Service

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Secret;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DatabaseService {
    
    @Secret("DATABASE_PASSWORD")
    private String password;
    
    @Secret("DATABASE_URL")
    private String url;
    
    @PostConstruct
    void init() {
        Aletheia.injectSecrets(this);
        // Validate
        if (password == null || url == null) {
            throw new IllegalStateException("Database configuration incomplete");
        }
    }
    
    public String getConnectionString() {
        return url + "?password=" + password;
    }
}
```

## Troubleshooting

- **Secrets not injected**: Ensure `@PostConstruct` method calls `Aletheia.injectSecrets(this)`
- **Provider not working**: Check Quarkus configuration properties
- **Native image issues**: Verify classes are included in native image

See [Common Issues](Common-Issues.md) for more help.

