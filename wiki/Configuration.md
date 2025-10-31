# Configuration

Aletheia can be configured through multiple methods with different priority levels.

## Configuration Methods

Configuration is loaded in the following priority order (highest to lowest):

1. **System Properties** (`-Dproperty=value`)
2. **Environment Variables**
3. **`aletheia.properties` file** (in classpath)

## Core Configuration

### Provider Chain

Configure which providers to use and their order:

```properties
aletheia.providers=VAULT,AWS,GCP,FILE,ENV
```

**Available providers:**
- `ENV` - Environment variables and system properties
- `FILE` - JSON file-based secrets
- `AWS` - AWS Secrets Manager
- `GCP` - Google Cloud Secret Manager
- `VAULT` - HashiCorp Vault

Providers are tried in the order specified. The first provider that returns a secret wins.

### Cache Configuration

Configure TTL-based caching:

```properties
# Cache TTL in seconds (default: 3600)
aletheia.cache.ttl.seconds=1800
```

Set to `0` to disable caching (not recommended for production).

## Provider-Specific Configuration

### ENV Provider

No configuration needed. Automatically reads:
- System properties (via `-Dkey=value`)
- Environment variables

### FILE Provider

```properties
# Path to JSON secrets file
file.path=/path/to/secrets.json
```

Or via system property:
```bash
java -Dfile.path=/path/to/secrets.json MyApp
```

### AWS Secrets Manager

```properties
# Required
AWS_REGION=us-east-1

# Optional (if not using IAM role/credentials file)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# Optional: For local testing (LocalStack, etc.)
AWS_ENDPOINT_URL=http://localhost:4566
```

### GCP Secret Manager

```properties
# Required
GCP_PROJECT_ID=my-project-id

# Optional: Path to credentials file
GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

Or via environment variables:
```bash
export GCP_PROJECT_ID=my-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### HashiCorp Vault

```properties
# Required
vault.url=http://vault.example.com:8200
vault.token=your-vault-token

# Optional: Default path prefix
vault.path=secret/data/myapp
```

## Configuration Files

### aletheia.properties

Create `aletheia.properties` in your classpath (e.g., `src/main/resources/`):

```properties
# Provider chain
aletheia.providers=VAULT,FILE,ENV

# Cache settings
aletheia.cache.ttl.seconds=3600

# File provider
file.path=/etc/secrets/secrets.json

# Vault configuration
vault.url=https://vault.example.com
vault.token=${VAULT_TOKEN}
vault.path=secret/data/myapp

# AWS configuration
AWS_REGION=us-east-1

# GCP configuration
GCP_PROJECT_ID=my-project-id
```

### Spring Boot application.properties

When using Spring Boot, you can configure Aletheia in `application.properties`:

```properties
# Aletheia configuration
aletheia.providers=VAULT,FILE,ENV
aletheia.cache.ttl.seconds=1800
file.path=/path/to/secrets.json
vault.url=${VAULT_URL}
vault.token=${VAULT_TOKEN}
```

### Profile-Specific Configuration

Use Spring profiles for environment-specific configuration:

**application-dev.properties:**
```properties
aletheia.providers=FILE,ENV
file.path=./secrets-dev.json
```

**application-prod.properties:**
```properties
aletheia.providers=VAULT,AWS,ENV
vault.url=https://vault.prod.example.com
AWS_REGION=us-east-1
```

## Programmatic Configuration

You can also configure Aletheia programmatically:

```java
import io.github.mwangiiharun.aletheia.Aletheia;

// Set system properties before first use
System.setProperty("aletheia.providers", "ENV,FILE");
System.setProperty("file.path", "/path/to/secrets.jsonналоги");

// Reinitialize with new configuration
Aletheia.reinitializeProviders();
```

## Environment Variable Interpolation

Aletheia supports environment variable interpolation in configuration files:

```properties
vault.token=${VAULT_TOKEN}
file.path=${SECRETS_FILE_PATH}
GCP_PROJECT_ID=${GCP_PROJECT_ID}
```

## Validation

Aletheia validates configuration onInit:
- Unknown providers are logged and skipped
- Missing required configuration for active providers is logged
- Invalid cache TTL values default to 3600 seconds

## Next Steps

- [Provider Chain](Provider-Chain.md) - Learn how provider fallback works
- [Providers](Providers.md) - Detailed provider documentation

