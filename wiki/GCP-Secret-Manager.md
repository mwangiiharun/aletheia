# GCP Secret Manager

Complete guide for using Google Cloud Secret Manager as a secret provider.

## Overview

The GCP provider fetches secrets from Google Cloud Secret Manager.

## Prerequisites

- Google Cloud Project
- Secret Manager API enabled
- Service account with Secret Accessor role
- Service account credentials JSON file

## Configuration

### Basic Configuration

```properties
GCP_PROJECT_ID=my-project-id
GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
aletheia.providers=GCP
```

### Environment Variables

```bash
export GCP_PROJECT_ID=my-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### System Properties

```bash
java -DGCP_PROJECT_ID=my-project-id \
     -DGOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json \
     MyApp
```

## Service Account Setup

### Step 1: Create Service Account

```bash
gcloud iam service-accounts create aletheia-sa \
  --display-name="Aletheia Service Account"
```

### Step 2: Grant Secret Accessor Role

```bash
gcloud projects add-iam-policy-binding my-project-id \
  --member="serviceAccount:aletheia-sa@my-project-id.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### Step 3: Create and Download Key

```bash
gcloud iam service-accounts keys create credentials.json \
  --iam-account=aletheia-sa@my-project-id.iam.gserviceaccount.com
```

## Credentials File Format

The credentials file should be a JSON file with the following structure:

```json
{
  "type": "service_account",
  "project_id": "my-project-id",
  "private_key_id": "key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "aletheia-sa@my-project-id.iam.gserviceaccount.com",
  "client_id": "123456789",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  ...
}
```

## Enable Secret Manager API

```bash
gcloud services enable secretmanager.googleapis.com --project=my-project-id
```

## Usage

### Secret Naming

Aletheia uses the secret ID directly:

```java
@Secret("my-database-password")
private String password;

// Queries GCP Secret Manager for secret ID "my-database-password"
```

### Create Secret in GCP

```bash
# Create secret
echo -n "my-secret-value" | gcloud secrets create my-database-password \
  --data-file=- \
  --project=my-project-id

# Grant access to service account
gcloud secrets add-iam-policy-binding my-database-password \
  --member="serviceAccount:aletheia-sa@my-project-id.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project=my-project-id
```

### Secret Versions

GCP Secret Manager uses versions. Aletheia accesses the latest version automatically.

## Local Development

### Application Default Credentials

Use ADC for local development:

```bash
gcloud auth application-default login
```

Then no need to set `GOOGLE_APPLICATION_CREDENTIALS`.

### Emulator (Future)

GCP Secret Manager emulator support may be added in future versions.

## Error Handling

### Common Errors

**PermissionDeniedException**:
- Service account lacks Secret Accessor role
- Secret doesn't exist
- Check IAM bindings

**NotFoundException**:
- Secret doesn't exist
- Check secret ID spelling
- Verify project ID

### Handle Errors

```java
try {
    String secret = Aletheia.getSecret("GCP_SECRET");
} catch (ProviderException e) {
    // Handle GCP-specific errors
}
```

## Best Practices

### 1. Use Service Accounts

Always use service accounts, never user accounts:
- Better security
- Easier to manage permissions
- No user credentials to rotate

### 2. Scope Permissions

Grant Secret Accessor only to specific secrets:
```bash
gcloud secrets add-iam-policy-binding my-secret \
  --member="serviceAccount:sa@project.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### 3. Use Secret Naming Conventions

Organize secrets with naming:
- `myapp-database-password`
- `myapp-api-key-prod`

### 4. Enable Secret Rotation

Use GCP's rotation features for automatic secret updates.

### 5. Use Provider Chain

```properties
aletheia.providers=GCP,FILE,ENV
```

## Troubleshooting

### Authentication Fails

1. Verify credentials file path: `echo $GOOGLE_APPLICATION_CREDENTIALS`
2. Check credentials file is valid JSON
3. Verify service account exists

### Secret Not Found

1. List secrets: `gcloud secrets list --project=my-project-id`
2. Check secret ID matches exactly
3. Verify project ID is correct

### Permission Errors

1. Check IAM binding: `gcloud secrets get-iam-policy my-secret`
2. Verify service account has Secret Accessor role
3. Check project-level permissions

## Cost Considerations

GCP Secret Manager pricing:
- $0.06 per secret version per month
- $0.03 per 10,000 access operations

**Minimize costs:**
- Use caching (configure TTL)
- Use FILE provider for development
- Cache aggressively in production

## See Also

- [Providers](Providers.md) - General provider information
- [Provider Chain](Provider-Chain.md) - Using GCP with other providers
- [Common Issues](Common-Issues.md) - Troubleshooting

