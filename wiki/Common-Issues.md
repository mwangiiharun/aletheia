# Common Issues

Solutions to frequently encountered problems when using Aletheia.

## Secrets Not Being Injected

### Problem
Secrets are not being injected into fields annotated with `@Secret`.

### Solutions

**1. Verify the class is a Spring bean:**
```java
@Component  // ✅ Required for Spring Boot
public class MyService {
    @Secret("MY_SECRET")
    private String secret;
}
```

**2. Check provider configuration:**
```properties
aletheia.providers=ENV
```

**3. Verify secret exists in provider:**
```bash
echo $MY_SECRET  # For ENV provider
```

**4. Check if field is static or final:**
Static and final fields are skipped by Aletheia.

## Provider Not Working

### Problem
A configured provider (AWS, GCP, Vault) is not retrieving secrets.

### Solutions

**AWS Secrets Manager:**
- Verify `AWS_REGION` is set
- Check AWS credentials are configured
- Ensure IAM permissions for Secrets Manager
- For local testing, set `AWS_ENDPOINT_URL`

**GCP Secret Manager:**
- Verify `GCP_PROJECT_ID` is set
- Check `GOOGLE_APPLICATION_CREDENTIALS` points to valid credentials
- Ensure Secret Manager API is enabled
- Check service account has Secret Accessor role

**Vault:**
- Verify `vault.url` is accessible
- Check `vault.token` is valid and not expired
- Ensure vault path exists and token has read permissions
- Check vault path format: `secret/data/myapp` (KV v2)

**File Provider:**
- Verify `file.path` points to existing file
- Check file format is valid JSON
- Ensure application has read permissions

## Circular Reference Error

### Problem
Getting `CircularSecretReferenceException`.

### Cause
A secret's value references another secret, which references back to the first secret.

### Solution
Break the circular dependency by:
1. Using a direct value instead of referencing another secret
2. Restructuring your secret hierarchy
3. Using different secret keys

## ClassNotFoundException / NoClassDefFoundError

### Problem
Runtime errors about missing classes.

### Solution
Ensure all required dependencies are included:
- AWS SDK v2 (for AWS provider)
- GCP Secret Manager library (for GCP provider)
- Spring Boot starter (for Spring Boot integration)

## Cache Not Updating

### Problem
Secrets are cached and changes aren't reflected immediately.

### Solution
**Option 1: Reduce cache TTL**
```properties
aletheia.cache.ttl.seconds=60
```

**Option 2: Clear cache programmatically**
```java
Aletheia.reinitializeProviders();  // Clears cache
```

**Option 3: Disable cache for specific providers**
Use a non-cached provider implementation (not recommended for production).

## Spring Boot Auto-Configuration Not Working

### Problem
Aletheia is not auto-configuring in Spring Boot.

### Solution
**1. Check dependency:**
```xml
<dependency>
    <groupId>io.github.mwangiiharun</groupId>
    <artifactId>aletheia</artifactId>
</dependency>
```

**2. Verify Spring Boot version:**
Aletheia works with Spring Boot 2.x and 3.x.

**3. Use explicit configuration:**
```java
@Configuration
@EnableAletheia
public class AppConfig {
}
```

## Secrets from Wrong Provider

### Problem
Secrets are being retrieved from an unexpected provider.

### Solution
**1. Check provider order:**
```properties
aletheia.providers=VAULT,FILE,ENV
```
Providers are tried in order, first match wins.

**2. Verify provider configuration:**
Ensure each provider is configured correctly.

**3. Use provider-specific keys:**
If needed, use different keys for different providers to avoid conflicts.

## Performance Issues

### Problem
Secret retrieval is slow.

### Solution
**1. Enable caching:**
```properties
aletheia.cache.ttl.seconds=3600
```

**2. Order providers by speed:**
Put faster providers first (ENV, FILE before cloud providers).

**3. Reduce network latency:**
Use regional endpoints for cloud providers.

**4. Check provider health:**
Verify cloud provider services are responding quickly.

## Missing Environment Variables

### Problem
Environment variables are not being read.

### Solution
**1. Verify variable is set:**
```bash
echo $MY_SECRET
```

**2. Check if using system properties instead:**
```bash
java -DMY_SECRET=value MyApp
```

**3. Verify ENV provider is configured:**
```properties
aletheia.providers=ENV
```

## Vault 403 Forbidden

### Problem
Getting 403 errors from Vault.

### Solution
**1. Check token permissions:**
Verify token has read access to the vault path.

**2. Verify path format:**
- KV v1: `secret/myapp`
- KV v2: `secret/data/myapp`

**3. Check token expiration:**
Tokens may expire - refresh if needed.

## AWS Access Denied

### Problem
Getting access denied from AWS Secrets Manager.

### Solution
**1. Verify IAM permissions:**
```json
{
  "Effect": "Allow",
  "Action": [
    "secretsmanager:GetSecretValue"
  ],
  "Resource": "arn:aws:secretsmanager:*:*:secret:*"
}
```

**2. Check credentials:**
Verify AWS credentials are correct and active.

**3. Verify region:**
Ensure `AWS_REGION` matches where the secret is stored.

## Getting Help

If you're still experiencing issues:

1. Check the [Debugging](Debugging.md) guide
2. Review [Best Practices](Best-Practices.md)
3. Open an issue on [GitHub](https://github.com/mwangiiharun/aletheia/issues)

When reporting issues, include:
- Aletheia version
- Java version
- Provider configuration (without secrets!)
- Error messages and stack traces
- Steps to reproduce

