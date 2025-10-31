# Migration Guide

Guide for migrating between Aletheia versions or from other secret management solutions.

## Version Migration

### From 0.1.x to 0.2.x

#### Namespace Change

**Old (0.1.x):**
```java
import com.aletheia.Aletheia;
import com.aletheia.annotations.Secret;
```

**New (0.2.x):**
```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Secret;
```

#### Maven Coordinates

**Old:**
```xml
<dependency>
    <groupId>com.aletheia</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.1.0</version>
</dependency>
```

**New:**
```xml
<dependency>
    <groupId>io.github.mwangiiharun</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.2.0</version>
</dependency>
```

#### Migration Steps

1. **Update dependency** in `pom.xml` or `build.gradle`
2. **Update imports** - Replace all `com.aletheia` with `io.github.mwangiiharun.aletheia`
3. **Update Spring configuration** if using Spring Boot:
   - `spring.factories`: Update auto-configuration class name
   - `AutoConfiguration.imports`: Update package
4. **Test thoroughly** - Verify all secrets are resolved correctly

#### Automatic Migration (IDE)

Most IDEs can help with find-and-replace:

**Find:** `com.aletheia`
**Replace:** `io.github.mwangiiharun.aletheia`

## Migrating from Other Solutions

### From Spring Cloud Config

**Before (Spring Cloud Config):**
```java
@Value("${database.password}")
private String password;
```

**After (Aletheia):**
```java
@Secret("database.password")
private String password;
```

**Configuration:**
```properties
# Remove
# spring.cloud.config.uri=...

# Add
aletheia.providers=VAULT,ENV
```

### From AWS Parameter Store (SSM)

**Before:**
```java
// Using AWS SDK directly
String value = ssmClient.getParameter(
    GetParameterRequest.builder()
        .name("/myapp/database/password")
        .withDecryption(true)
        .build()
).parameter().value();
```

**After:**
```java
@Secret("database.password")
private String password;
```

**Configuration:**
```properties
AWS_REGION=us-east-1
aletheia.providers=AWS
```

Note: Aletheia uses Secrets Manager, not Parameter Store. Migrate secrets to Secrets Manager first.

### From Environment Variables Only

**Before:**
```java
String password = System.getenv("DATABASE_PASSWORD");
```

**After:**
```java
@Secret("DATABASE_PASSWORD")
private String password;
```

**Configuration:**
```properties
aletheia.providers=ENV
# Works the same, but now with provider chain support
```

### From Custom Secret Manager

**Before:**
```java
String password = MySecretManager.getSecret("database.password");
```

**After:**

**Option 1: Use Aletheia directly**
```java
@Secret("database.password")
private String password;
```

**Option 2: Create custom provider**
```java
// Implement SecretProvider interface
// See [Custom Providers](Custom-Providers.md) guide
```

## Migration Strategy

### Phase 1: Parallel Run

Run both systems in parallel:

```java
// Old way
String oldSecret = MyOldSystem.getSecret("KEY");

// New way (test)
try {
    String newSecret = Aletheia.getSecret("KEY");
    if (!oldSecret.equals(newSecret)) {
        log.warn("Secret mismatch for KEY");
    }
} catch (Exception e) {
    log.error("Aletheia failed, using old system", e);
    // Fallback to old system
}
```

### Phase 2: Gradual Migration

1. Start with non-critical secrets
2. Test in dev/staging environments
3. Migrate one service at a time
4. Monitor for errors

### Phase 3: Complete Migration

1. Remove old secret management code
2. Update all references to use Aletheia
3. Remove old dependencies
4. Update documentation

## Configuration Migration

### Migrating Configuration Files

**Old (custom):**
```properties
secrets.vault.url=http://vault:8200
secrets.vault.token=token
secrets.file.path=./secrets.json
```

**New (Aletheia):**
```properties
vault.url=http://vault:8200
vault.token=token
file.path=./secrets.json
aletheia.providers=VAULT,FILE
```

### Migrating Spring Boot Configuration

**Old:**
```yaml
spring:
  cloud:
    config:
      uri: http://config-server:8888
```

**New:**
```properties
aletheia.providers=VAULT,FILE,ENV
vault.url=http://vault:8200
```

## Testing Migration

### Pre-Migration Checklist

- [ ] List all secrets used by application
- [ ] Document current secret sources
- [ ] Verify secrets exist in new provider
- [ ] Test provider connectivity
- [ ] Create rollback plan

### Migration Testing

```java
@Test
void testSecretMigration() {
    // Test each secret
    List<String> secretKeys = Arrays.asList(
        "DATABASE_PASSWORD",
        "API_KEY",
        "JWT_SECRET"
    );
    
    for (String key : secretKeys) {
        try {
            String value = Aletheia.getSecret(key);
            assertNotNull(value, "Secret " + key + " should be resolved");
            assertFalse(value.isEmpty(), "Secret " + key + " should not be empty");
        } catch (Exception e) {
            fail("Failed to resolve secret: " + key, e);
        }
    }
}
```

## Rollback Plan

If migration fails:

1. **Revert dependency** to old version
2. **Restore old configuration**
3. **Deploy previous version**
4. **Investigate issues**
5. **Fix and retry migration**

## Common Migration Issues

### Issue: Secret Not Found

**Cause**: Secret key name mismatch

**Solution**: 
- Verify secret exists in provider
- Check key name spelling
- Use provider-specific naming conventions

### Issue: Provider Not Working

**Cause**: Configuration or credentials issue

**Solution**:
- Verify provider configuration
- Check credentials/authentication
- Test provider connectivity

### Issue: Performance Degradation

**Cause**: Network latency, no caching

**Solution**:
- Enable caching: `aletheia.cache.ttl.seconds=3600`
- Use faster providers first
- Consider local providers for development

## Getting Help

- Check [Common Issues](Common-Issues.md) for solutions
- Review [Debugging](Debugging.md) guide
- Open issue on [GitHub](https://github.com/mwangiiharun/aletheia/issues)

