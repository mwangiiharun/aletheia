# HashiCorp Vault

Complete guide for using HashiCorp Vault as a secret provider.

## Overview

The Vault provider fetches secrets from HashiCorp Vault using HTTP API.

## Prerequisites

- HashiCorp Vault server (local or remote)
- Valid Vault token
- Appropriate Vault policy permissions

## Configuration

### Basic Configuration

```properties
vault.url=http://vault.example.com:8200
vault.token=your-vault-token
aletheia.providers=VAULT
```

### With Path Prefix

```properties moderator
vault.url=http://vault.example.com:8200
vault.token=your-vault-token
vault.path=secret/data/myapp
aletheia.providers=VAULT
```

### Environment Variables

```bash
export VAULT_URL=http://vault.example.com:8200
export VAULT_TOKEN=your-vault-token
export VAULT_PATH=secret/data/myapp
```

## Vault Setup

### Step 1: Start Vault

**Development (local):**
```bash
vault server -dev
```

**Production:**
```bash
# Use proper Vault configuration
vault server -config=vault.hcl
```

### Step 2: Authenticate

```bash
# Development token (from -dev output)
export VAULT_TOKEN=hvs.xxxxx

# Production: Use auth method
vault auth -method=userpass username=myuser
```

### Step 3: Write Secrets

**KV v1:**
```bash
vault kv put secret/myapp database_password=secret123
```

**KV v2 (recommended):**
```bash
vault kv put secret/data/myapp database_password=secret123
```

## Secret Path Resolution

### Without Path Prefix

```java
@Secret("database_password")
private String password;

// Queries: http://vault:8200/v1/secret/data/database_password
```

### With Path Prefix

```properties
vault.path=secret/data/myapp
```

```java
@Secret("database_password")
private String password;

// Queries: http://vault:8200/v1/secret/data/myapp/database_password
```

## KV Engine Versions

### KV v1

Path format: `secret/myapp`

```bash
vault kv put secret/myapp key=value
```

Configuration:
```properties
vault.url=http://vault:8200
vault.token=token
vault.path=secret
```

### KV v2 (Recommended)

Path format: `secret/data/myapp`

```bash
vault kv put secret/data/myapp key=value
```

Configuration:
```properties
vault.url=http://vault:8200
vault.token=token
vault.path=secret/data/myapp
```

## Vault Policies

### Create Policy

```hcl
# myapp-policy.hcl
path "secret/data/myapp/*" {
  capabilities = ["read"]
}
```

```bash
vault policy write myapp-policy myapp-policy.hcl
```

### Assign Policy to Token

```bash
vault token create -policy=myapp-policy
```

## Authentication Methods

### Token Authentication (Simple)

```properties
vault.token=hvs.xxxxx
```

### AppRole (Production Recommended)

```bash
# Enable AppRole
vault auth enable approle

# Create role
vault write auth/approle/role/myapp \
  token_policies=myapp-policy \
  token_ttl=1h

# Get role-id and secret-id
vault read auth/approle/role/myapp/role-id
vault write -f auth/approle/role/myapp/secret-id
```

Note: Aletheia currently supports token auth. AppRole may require custom implementation.

## Local Development

### Vault Dev Server

```bash
# Start dev server
vault server -dev

# Export token and address
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=hvs.xxxxx

# Write test secret
vault kv put secret/data/myapp test_secret=test-value
```

### Docker

```bash
docker run -d --name vault \
  -p 8200:8200 \
  -e VAULT_DEV_ROOT_TOKEN_ID=myroot \
  vault

export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=myroot
```

## Error Handling

### Common Errors

**403 Forbidden**:
- Token lacks permissions
- Check Vault policy
- Verify path access

**404 Not Found**:
- Secret doesn't exist
- Check path format
- Verify KV engine version

**500 Internal Server Error**:
- Vault server error
- Check Vault logs
- Verify Vault health

### Handle Errors

```java
try {
    String secret = Aletheia.getSecret("VAULT_SECRET");
} catch (ProviderException e) {
    // Check HTTP status in exception cause
}
```

## Best Practices

### 1. Use KV v2

KV v2 provides:
- Versioning
- Metadata
- Better security

### 2. Scope Policies

Grant minimal permissions:
```hcl
path "secret/data/myapp/*" {
  capabilities = ["read"]
}
```

### 3. Use Path Prefixes

Organize secrets:
- `secret/data/myapp/database`
- `secret/data/myapp/api`

### 4. Rotate Tokens

Regularly rotate Vault tokens:
```bash
vault token renew
```

### 5. Use Provider Chain

```properties
aletheia.providers=VAULT,FILE,ENV
```

## Troubleshooting

### Connection Errors

1. Check Vault URL: `curl $VAULT_ADDR/v1/sys/health`
2. Verify network connectivity
3. Check firewall rules

### Authentication Errors

1. Verify token: `vault token lookup`
2. Check token is not expired
3. Verify token has required policies

### Permission Errors

1. Test policy: `vault token capabilities secret/data/myapp/test`
2. Check policy assignment
3. Verify path in policy matches secret path

## Security Considerations

### Token Security

- Never commit tokens to version control
- Use environment variables or secret management
- Rotate tokens regularly
- Use short TTL tokens when possible

### Network Security

- Use HTTPS in production
- Verify TLS certificates
- Use Vault's mTLS for additional security

## See Also

- [Providers](Providers.md) - General provider information
- [Provider Chain](Provider-Chain.md) - Using Vault with other providers
- [Common Issues](Common-Issues.md) - Troubleshooting

