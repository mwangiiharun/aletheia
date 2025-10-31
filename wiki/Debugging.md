# Debugging

Tips and techniques for debugging secret resolution issues.

 LEFT
## Enable Debug Logging

### Logback Configuration

**logback.xml:**
```xml
<configuration>
    <logger name="io.github.mwangiiharun.aletheia" level="DEBUG"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### Log4j2 Configuration

**log4j2.xml:**
```xml
<Configuration>
    <Loggers>
        <Logger name="io.github.mwangiiharun.aletheia" level="DEBUG"/>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### Java Util Logging

```properties
# logging.properties
io.github.mwangiiharun.aletheia.level=FINE
```

## Debug Information

### Provider Initialization

With DEBUG logging, you'll see:
```
DEBUG - Initializing providers: [ENV, FILE]
DEBUG - Provider ENV initialized
DEBUG - Provider FILE initialized with path: ./secrets.json
```

### Secret Resolution

```
DEBUG - Resolving secret: DATABASE_PASSWORD
DEBUG - Trying provider: ENV
DEBUG - Provider ENV returned null
DEBUG - Trying provider: FILE
DEBUG - Provider FILE returned value
```

### Cache Behavior

```
DEBUG - Cache hit for key: DATABASE_PASSWORD
DEBUG - Cache miss for key: API_KEY, querying provider
```

## Common Debugging Scenarios

### Scenario 1: Secret Not Found

**Enable debug logging and check:**

```
DEBUG - Resolving secret: MY_SECRET
DEBUG - Trying provider: ENV
DEBUG - Provider ENV returned null
DEBUG - Trying provider: FILE
DEBUG - Provider FILE returned null
ERROR - Secret MY_SECRET not found in any provider
```

**Solutions:**
1. Verify secret exists in configured providers
2. Check provider order
3. Verify provider configuration

### Scenario 2: Provider Not Initialized

**Check logs for:**
```
WARN - Unknown provider type 'INVALID'; defaulting to EnvProvider
```

**Solutions:**
1. Check provider name spelling
2. Verify provider is supported
3. Check provider configuration

### Scenario 3: Cache Issues

**Check for:**
```
DEBUG - Using cached value for: MY_SECRET (age: 3500s)
```

**Solutions:**
1. Clear cache: `Aletheia.reinitializeProviders()`
2. Reduce TTL if needed
3. Check if file/secret was updated

## Inspection Methods

### List Active Providers

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.SecretProvider;

List<SecretProvider> providers = Aletheia.getProviders();
for (SecretProvider provider : providers) {
    System.out.println("Provider: " + provider.getClass().getSimpleName());
}
```

### Test Provider Directly

```java
// Test ENV provider
EnvProvider envProvider = new EnvProvider();
String value = envProvider.getSecret("MY_SECRET");
System.out.println("ENV provider returned: " + value);

// Test FILE provider
FileProvider fileProvider = new FileProvider();
value = fileProvider.getSecret("MY_SECRET");
System.out.println("FILE provider returned: " + value);
```

### Check Provider Support

```java
SecretProvider provider = Aletheia.getProviders().get(0);
boolean supports = provider.supports("MY_SECRET");
System.out.println("Provider supports MY_SECRET: " + supports);
```

## Diagnostic Utilities

### Configuration Checker

```java
import io.github.mwangiiharun.aletheia.config.AletheiaConfig;

System.out.println("Providers: " + AletheiaConfig.getProviderOrder());
System.out.println("Cache TTL: " + AletheiaConfig.get("aletheia.cache.ttl.seconds"));
System.out.println("File path: " + AletheiaConfig.get("file.path"));
```

### Secret Resolution Tracer

```java
public class SecretTracer {
    public static void traceSecret(String key) {
        System.out.println("Tracing secret: " + key);
        
        List<SecretProvider> providers = Aletheia.getProviders();
        for (SecretProvider provider : providers) {
            System.out.println("  Trying: " + provider.getClass().getSimpleName());
            try {
                String value = provider.getSecret(key);
                if (value != null) {
                    System.out.println("  ✓ Found in " + provider.getClass().getSimpleName());
                    return;
                }
            } catch (Exception e) {
                System.out.println("  ✗ Error: " + e.getMessage());
            }
        }
        System.out.println("  ✗ Not found in any provider");
    }
}
```

## Provider-Specific Debugging

### ENV Provider

```java
// Check system property
System.out.println("System property: " + System.getProperty("MY_SECRET"));

// Check environment variable
System.out.println("Environment variable: " + System.getenv("MY_SECRET"));
```

### FILE Provider

```java
// Check file exists
File file = new File(System.getProperty("file.path"));
System.out.println("File exists: " + file.exists());
System.out.println("File readable: " + file.canRead());

// Check file contents
String content = Files.readString(file.toPath());
System.out.println("File content: " + content);
```

### AWS Provider

```bash
# Test AWS CLI
aws secretsmanager get-secret-value --secret-id my-secret --region us-east-1

# Check credentials
aws sts get-caller-identity
```

### GCP Provider

```bash
# Test GCP CLI
gcloud secrets versions access latest --secret="my-secret"

# Check credentials
gcloud auth list
```

### Vault Provider

```bash
# Test Vault CLI
vault kv get secret/data/myapp/my-secret

# Check token
vault token lookup
```

## Breakpoints and Step Debugging

### Common Breakpoints

1. `Aletheia.getSecret()` - Entry point
2. `CachedProvider.getSecret()` - Cache check
3. `Provider.getSecret()` - Provider query
4. `Aletheia.injectSecrets()` - Injection entry

### Watch Variables

- `key` - Secret key being resolved
- `providers` - List of active providers
- `value` - Resolved secret value (be careful not to log!)

## Troubleshooting Checklist

- [ ] DEBUG logging enabled
- [ ] Provider list is correct
- [ ] Provider configuration verified
- [ ] Secret exists in provider
- [ ] Cache cleared if needed
- [ ] Network connectivity (cloud providers)
- [ ] Credentials valid (cloud providers)
- [ ] File permissions (FILE provider)
- [ ] Environment variables set (ENV provider)

## See Also

- [Common Issues](Common-Issues.md) - Solutions to common problems
- [Error Handling](Error-Handling.md) - Understanding exceptions
- [Best Practices](Best-Practices.md) - Debugging best practices

