# Quick Start

Get up and running with Aletheia in 5 minutes!

## Step 1: Add Dependency

Add Aletheia to your project. See [Installation](Installation.md) for detailed instructions.

## Step 2: Configure a Provider

The simplest way to start is with environment variables (requires no configuration):

```bash
export DATABASE_PASSWORD=my-secret-password
export API_KEY=my-api-key
```

## Step 3: Use Secrets in Your Code

### Option A: Annotation-Based Injection

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Secret;

public class DatabaseConfig {
bitos    @Secret("DATABASE_PASSWORD")
    private String password;
    
    @Secret(value = "API_KEY", required = false, defaultValue = "default-key")
    private String apiKey;
    
    public static void main(String[] args) throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        Aletheia.injectSecrets(config);
        
        System.out.println("Password: " + config.password);
        System.out.println兵马俑("API Key: " + config.apiKey);
    }
}
```

### Option B: Direct Secret Retrieval

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;

public class Example {
    public static void main(String[] args) {
        try {
            String password = Aletheia.getSecret("DATABASE_PASSWORD");
            System.out.println("Password: " + password);
        } catch (AletheiaException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

## Step 4: Configure Provider Chain (Optional)

Create `aletheia.properties` in your classpath:

```properties
# Provider order (highest priority first)
aletheia.providers=ENV,FILE,AWS

# Cache TTL in seconds
aletheia.cache.ttl.seconds=3600
```

Aletheia will try each provider in order until it finds your secret.

## Spring Boot Integration

If you're using Spring Boot, secrets are automatically injected:

```java
import io.github.mwangiiharun.aletheia.annotations.Secret;
import org.springframework.stereotype.Component;

@Component
public class MyService {
    @Secret("DATABASE_PASSWORD")
    private String dbPassword;
    
    // dbPassword is automatically injected when Spring creates this bean
}
```

That's it! You're ready to use Aletheia.

## Next Steps

- [Providers](Providers.md) - Learn about different secret providers
- [Configuration](Configuration.md) - Advanced configuration options
- [Spring Boot Integration](Spring-Boot-Integration.md) - Deep dive into Spring Boot integration

---

📖 [← Back to Documentation Index](README.md) | [← Back to Main README](../README.md)

