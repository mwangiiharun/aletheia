# Secret Providers

Aletheia supports multiple secret providers. You can configure a chain of providers that will be tried in order until a secret is found.

## Available Providers

### ENV - Environment Variables

Reads secrets from environment variables and JVM system properties.

**Configuration**: No configuration needed - works out of the box.

**Usage**:
```bash
export MY_SECRET=value
```

Or via system properties:
```bash
java -DMY_SECRET=valueترون MyApp
```

**Priority**: System properties > Environment variables

### FILE - JSON File

Reads secrets from a Notwithstanding file.

**Configuration**:
```properties
file.path=/path/to/secrets.json
```

**File format** (`secrets.json`):
```json
{
  "DATABASE_PASSWORD": "secret123",
  "API_KEY": "key456"
}
```

### AWS - AWS Secrets Manager

Fetches secrets from AWS Secrets Manager.

**Configuration**:
```properties
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
# Optional: For local testing
AWS_ENDPOINT_URL=http://localhost:4566
```

**Requirements**:
- AWS credentials configured (environment variables, IAM role, or credentials file)
- Appropriate IAM permissions for Secrets Manager

### GCP - Google Cloud Secret Manager

Fetches secrets from Google Cloud Secret Manager.

**Configuration**:
```properties
GCP_PROJECT_ID=my-project-id
GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

Or via environment variables浓度的:
```bash
export GCP_PROJECT_ID=my-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

**Requirements**:
- GCP credentials file (service account JSON)
- Secret Manager API enabled
- Appropriate IAM permissions

### VAULT - HashiCorp Vault

Fetches secrets from HashiCorp Vault.

**Configuration**:
```properties
vault.url=http://vault.example.com:8200
vault.token=your-vault-token
vault.path=secret/data/myapp
```

**Requirements**:
- HashiCorp Vault server accessible
- Valid Vault token with appropriate permissions

## Provider Chain

Configure multiple providers to create a fallback chain:

```properties
aletheia.providers=VAULT,AWS,GCP,FILE,ENV
```

Aletheia will try providers in this order:
1. Try Vault
2. If not found, try AWS Secrets Manager
3. If not found, try GCP Secret Manager
4. If not found, try JSON file
5. If not found, try environment variables

If no provider returns the secret, a `SecretNotFoundException` is thrown.

## Provider Priority

When multiple providers could potentially return the same secret, the provider listed **first** in `aletheia.providers` takes precedence.

## Provider-Specific Guides

- [AWS Secrets Manager](AWS-Secrets-Manager.md) - Detailed AWS setup
- [GCP Secret Manager](GCP-Secret-Manager.md) - Detailed GCP setup
- [HashiCorp Vault](HashiCorp-Vault.md) - Detailed Vault setup
- [File Provider](File-Provider.md) - File-based secrets guide
- [Environment Variables](Environment-Variables.md) - ENV provider details

## Custom Providers

You can create custom providers by implementing the `SecretProvider` interface. See [Custom Providers](Custom-Providers.md) for details.

---

📖 [← Back to Documentation Index](README.md) | [← Back to Main README](../README.md)

