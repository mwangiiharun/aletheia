# Environment Variables

Complete guide for using environment variables and system properties as secrets.

## Overview

The ENV provider reads secrets from:
1. JVM System Properties (highest priority)
2. Environment Variables (fallback)

## Configuration

### No Configuration Needed

The ENV provider works out of the box - no configuration required!

```properties
aletheia.providers=ENV
```

## System Properties

Set via `-D` flag:

```bash
java -DDATABASE_PASSWORD=secret123 MyApp
```

Or programmatically:

```java
System.setProperty("DATABASE_PASSWORD", "secret123");
```

### Priority

System properties take precedence over environment variables:

```
System Property > Environment Variable
```

## Environment Variables

### Unix/Linux/macOS

```bash
export DATABASE_PASSWORD=secret123
export API_KEY=key456
java MyApp
```

### Windows (CMD)

```cmd
set DATABASE_PASSWORD=secret123
set API_KEY=key456
java MyApp
```

### Windows (PowerShell)

```powershell
$env:DATABASE_PASSWORD="secret123"
$env:API_KEY="key456"
java MyApp
```

### Docker

```dockerfile
ENV DATABASE_PASSWORD=secret123
ENV API_KEY=key456
```

Or at runtime:

```bash
docker run -e DATABASE_PASSWORD=secret123 -e API_KEY=key456 myapp
```

### Kubernetes

**ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  DATABASE_PASSWORD: secret123
```

**Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  DATABASE_PASSWORD: secret123
```

**Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: app
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets
```

## Usage

### Direct Retrieval

```java
import io.github.mwangiiharun.aletheia.Aletheia;

String password = Aletheia.getSecret("DATABASE_PASSWORD");
```

### Annotation-Based

```java
import io.github.mwangiiharun.aletheia.annotations.Secret;

public class Config {
    @Secret("DATABASE_PASSWORD")
    private String password;
}
```

## Best Practices

### 1. Use for Local Development

Perfect for quick local setup:
```bash
export DATABASE_PASSWORD=local-dev-password
```

### 2. Use System Properties for Testing

```java
@BeforeEach
void setup() {
    System.setProperty("TEST_SECRET", "test-value");
}
```

### 3. Always Include as Fallback

```properties
aletheia.providers=VAULT,AWS,FILE,ENV
```

### 4. Use Naming Conventions

Use descriptive, consistent names:
```bash
# ✅ Good
export MYAPP_DATABASE_PASSWORD=secret
export MYAPP_API_KEY=key

# ❌ Bad
export DB_PASS=secret
export KEY=key
```

## Common Patterns

### Development Setup Script

```bash
#!/bin/bash
# dev-setup.sh

export DATABASE_PASSWORD=dev-password
export API_KEY=dev-api-key
export LOG_LEVEL=DEBUG

java -jar myapp.jar
```

### Docker Compose

```yaml
version: '3.8'
services:
  app:
    image: myapp
    environment:
      - DATABASE_PASSWORD=${DATABASE_PASSWORD}
      - API_KEY=${API_KEY}
    env_file:
      - .env.local
```

### CI/CD

**GitHub Actions:**
```yaml
env:
  DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
  API_KEY: ${{ secrets.API_KEY }}

steps:
  - name: Run tests
    run: mvn test
```

## Security Considerations

### ✅ Safe

- Environment variables in containers
- System properties in tests
- Environment variables in CI/CD secrets

### ⚠️ Caution

- Don't log environment variable values
- Don't commit `.env` files to version control
- Use proper secret management in production

### ❌ Avoid

- Hardcoding secrets in code
- Committing secrets to version control
- Using environment variables for production secrets (use Vault/AWS/GCP)

## Limitations

### Variable Name Restrictions

- Must be valid Corpsvariable names
- No spaces or special characters (except `_`)
- Case-sensitive

### No Nested Structures

Environment variables are flat. For nested config, use FILE provider with JSON.

## Troubleshooting

### Variable Not Found

1. Check variable name matches exactly (case-sensitive)
2. Verify variable is set: `echo $VARIABLE_NAME`
3. Check if system property overrides it

### Variable Not Loading

1. Verify ENV provider is in provider list
2. Check variable is exported: `export VARIABLE_NAME=value`
3. Restart application after setting variables

## See Also

- [Providers](Providers.md) - General provider information
- [Provider Chain](Provider-Chain.md) - Using ENV with other providers
- [Common Issues](Common-Issues.md) - Troubleshooting

