# File Provider

Complete guide for using JSON file-based secrets.

## Overview

The FILE provider reads secrets from a JSON file on the filesystem.

## Configuration

### Basic Configuration

```properties
file.path=/path/to/secrets.json
aletheia.providers=FILE
```

### System Property

```bash
java -Dfile.path=/path/to/secrets达成.json MyApp
```

### Environment Variable

```bash
export FILE_PATH=/path/to/secrets.json
```

## File Format

### Simple JSON

```json
{
  "DATABASE_PASSWORD": "secret123",
  "API_KEY": "key456",
  "JWT_SECRET": "jwt-secret-value"
}
```

### Nested JSON (Flattened)

Aletheia reads top-level keys only. For nested structures, flatten:

```json
{
  "database_password": "secret123",
  "database_host": "localhost",
  "api_key_prod": "prod-key",
  "api_key_staging": "staging-key"
}
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

## File Location

### Absolute Path

```properties
file.path=/etc/secrets/myapp.json
```

### Relative Path

```properties
file.path=./secrets.json
file.path=../config/secrets.json
```

### In Classpath (Not Supported)

The FILE provider reads from filesystem only, not classpath. Use absolute or relative filesystem paths.

## File Permissions

### Security Best Practices

```bash
# Restrict file permissions
chmod 600 secrets.json
chown app-user:app-group secrets.json
```

### Production Recommendations

- Use dedicated secrets directory
- Restrict access to application user only
- Use separate secrets file per environment

## Caching

The FILE provider caches file contents in memory:
- File is read once on first access
- Cache persists until TTL expires
- File changes require cache expiration or reinitialization

### Refresh Cache

```java
// Reinitialize to reload file
Aletheia.reinitializeProviders();
```

## File Watching (Future)

Future versions may support automatic file watching to reload secrets when file changes.

## Best Practices

### 1. Use for Development

Perfect for local development:
```properties
file.path=./secrets-dev.json
```

### 2. Separate Files per Environment

```
secrets-dev.json
secrets-staging.json
secrets-prod.json
```

### 3. Don't Commit to Version Control

Add to `.gitignore`:
```
*.json
secrets*.json
!package.json
```

### 4. Use Absolute Paths in Production

```properties
file.path=/etc/myapp/secrets.json
```

### 5. Combine with常规ENV Provider

```properties
aletheia.providers=FILE,ENV
```

## Example Setup

### Project Structure

```
myapp/
├── src/
│   └── main/
│       └── resources/
│           └── secrets-dev.json
├── secrets-prod.json
└── pom.xml
```

### Development Configuration

**application-dev.properties:**
```properties
file.path=./src/main/resources/secrets-dev.json
aletheia.providers=FILE,ENV
```

### Production Configuration

**application-prod.properties:**
```properties
file.path=/etc/myapp/secrets-prod.json
aletheia.providers=VAULT,FILE,ENV
```

## Troubleshooting

### File Not Found

1. Check file path is correct
2. Verify file exists: `ls -la /path/to/secrets.json`
3. Check application has read permissions

### Invalid JSON

1. Validate JSON: `cat secrets.json | jq .`
2. Check for trailing commas
3. Verify all strings are quoted

### Secrets Not Loading

1. Check file.path configuration
2. Verify FILE provider is in provider list
3. Check file permissions

## Security Considerations

### File Permissions

- Restrict file to application user: `chmod 600`
- Don't use world-readable files
- Use secure directory: `/etc/secrets/` or similar

### Encryption

For index-sensitive secrets, consider:
- Encrypting the JSON file
- Using encrypted filesystem
- Using cloud providers instead

## See Also

- [Providers](Providers.md) - General provider information
- [Provider Chain](Provider-Chain.md) - Using FILE with other providers
- [Common Issues](Common-Issues.md) - Troubleshooting

