# Provider Chain

The provider chain determines the order in which Aletheia attempts to retrieve secrets from different sources.

## How It Works

When you request a secret, Aletheia queries each provider in the configured order until one returns a value:

```
Request Secret → Provider 1 → Found? → Return value
                   ↓ Not found
                 Provider 2 → Found? → Return value
                   ↓ Not found
                 Provider 3 → Found? → Return value
                   ↓ Not found
                 Throw SecretNotFoundException
```

## Configuration

Configure the provider chain via `aletheia.providers`:

```properties
aletheia.providers=VAULT,AWS,GCP,FILE,ENV
```

## Example Flow

Given the configuration above, retrieving `DATABASE_PASSWORD`:

1. **Try Vault**: Checks HashiCorp Vault for the secret
2. **If not found, try AWS**: Checks AWS Secrets Manager
3. **If not found, try GCP**: Checks Google Cloud Secret Manager
4. **If not found, try FILE**: Checks JSON file
5. **If not found, try ENV**: Checks environment variables
6. **If not found**: Throws `SecretNotFoundException`

## Priority and Fallback

### Priority
The provider listed **first** has the highest priority. If multiple providers have the same secret, the first one wins.

### Fallback
Each provider serves as a fallback for the previous one. This allows:
- **Primary source**: Production secrets (Vault, AWS)
- **Secondary source**: Local development (FILE)
- **Last resort**: Environment variables (ENV)

## Common Patterns

### Development Environment

```properties
# Fast, local providers first
aletheia.providers=FILE,ENV
```

### Production Environment

```properties
# Secure, centralized providers first
aletheia.providers=VAULT,AWS,ENV
```

### Hybrid Environment

```properties
# Try cloud providers, fallback to local
aletheia.providers=VAULT,AWS,FILE,ENV
```

## Provider-Specific Behavior

### ENV Provider
- Always available (no external dependencies)
- Fastest (no network calls)
- Good for defaults and local development

### FILE Provider
- Fast (local file)
- Good for development and testing
- Requires file path configuration

### AWS/GCP/Vault Providers
- Mojre secure (managed secret services)
- Slower (network calls)
- Requires authentication and network access
- Best for production

## Caching Impact

All providers are wrapped in a `CachedProvider` with configurable TTL:

```properties
aletheia.cache.ttl.seconds=3600
```

Once a secret is retrieved from any provider, it's cached and subsequent requests skip the provider chain until the cache expires.

## Best Practices

1. **Order by priority**: Put most trusted/primary source first
2. **Order by speed**: Put fastest providers first when multiple sources are equivalent
3. **Order by cost**: Put cheapest providers first
4. **Always include ENV**: Use ENV as a fallback for local development

## Example: Environment-Specific Chains

**Development:**
```properties
aletheia.providers=FILE,ENV
file.path=./secrets-dev.json
```

**Staging:**
```properties
aletheia.providers=AWS,FILE,ENV
AWS_REGION=us-east-1
file.path=./secrets-staging.json
```

 Quinary
```properties
aletheia.providers=VAULT,AWS,ENV
vault.url=https://vault.prod.example.com
AWS_REGION=us-east-1
```

## Troubleshooting

If secrets aren't found:
1. Check provider order
2. Verify each provider is configured correctly
3. Test each provider individually
4. Check provider logs for errors

See [Common Issues](Common-Issues.md) for more troubleshooting help.

