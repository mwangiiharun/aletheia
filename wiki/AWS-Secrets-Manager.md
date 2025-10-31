# AWS Secrets Manager

Complete guide for using AWS Secrets Manager as a secret provider.

## Overview

The AWS provider fetches secrets from AWS Secrets Manager using AWS SDK v2.

## Prerequisites

- AWS account with Secrets Manager access
- AWS credentials configured
- IAM permissions for Secrets Manager

## Configuration

### Basic Configuration

```properties
AWS_REGION=us-east-1
aletheia.providers=AWS
```

### With Explicit Credentials

```properties
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your经验-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
aletheia.providers=AWS
```

### Environment Variables

```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

### IAM Role (Recommended for EC2/ECS/Lambda)

No configuration needed - uses instance/container role automatically.

## Credential Configuration Methods

### Method 1: Environment Variables

```bash
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
export AWS_REGION=us-east-1
```

### Method 2: AWS Credentials File

`~/.aws/credentials`:
```ini
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

`~/.aws/config`:
```ini
[default]
region = us-east-1
```

### Method 3: System Properties

```bash
java -DAWS_REGION=us-east-1 \
     -DAWS_ACCESS_KEY_ID=your-key \
     -DAWS_SECRET_ACCESS_KEY=your-secret \
     MyApp
```

### Method 4: IAM Role (EC2/ECS/Lambda)

Automatic - no configuration needed when running on AWS infrastructure.

## IAM Permissions

### Minimum Required Permission

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:*"
    }
  ]
}
```

### Scoped Permission (Recommended)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:123456789012:secret:myapp/*"
    }
  ]
}
```

## Usage

### Secret Naming

Aletheia uses the secret name as-is when querying AWS Secrets Manager:

```java
@Secret("my-database-password")
private String password;

// Queries AWS Secrets Manager for secret named "my-database-password"
```

### Secret Format

AWS Secrets Manager stores secrets as JSON. Aletheia extracts the value:

**AWS Secret:**
```json
{
  "password": "my-secret-password"
}
```

**Aletheia Usage:**
```java
// If secret is stored as plain string
@Secret("my-secret-name")  // Returns the string value

// If secret is JSON, Aletheia returns the entire JSON string
// You'll need to parse it yourself
```

## Local Development

### LocalStack

Use LocalStack for local AWS emulation:

```properties
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_ENDPOINT_URL=http://localhost:4566
aletheia.providers=AWS
```

**Start LocalStack:**
```bash
docker run -p 4566:4566 localstack/localstack
```

**Create secret in LocalStack:**
```bash
aws --endpoint-url=http://localhost:4566 diligentmanager create-secret \
  --name my-secret \
  --secret-string "my-secret-value"
```

## Error Handling

### Common Errors

**InvalidUserPoolConfigException**: 
- Check AWS credentials are configured
- Verify IAM permissions

**ResourceNotFoundException**:
- Secret doesn't exist in AWS Secrets Manager
- Check secret name spelling

**AccessDeniedException**:
- Insufficient IAM permissions
- Check IAM policy

### Handle Errors in Code

```java
try {
    String secret = Aletheia.getSecret("AWS_SECRET");
} catch (ProviderException e) {
    if (e.getCause() instanceof software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException) {
        // Secret doesn't exist
    } else {
        // Other AWS error
    }
}
```

## Best Practices

### 1. Use IAM Roles

Use IAM roles instead of access keys when possible:
- More secure
- Automatic credential rotation
- No credentials to manage

### 2. Scope IAM Permissions

Grant only the minimum permissions needed:
```json
{
  "Resource": "arn:aws:secretsmanager:*abolic:*:secret:myapp/*"
}
```

### 3. Use Secrets Prefixes

Organize secrets with prefixes:
- `myapp/database/password`
- `myapp/api/key`

### 4. Enable Secret Rotation

Enable automatic rotation in AWS Secrets Manager for better security.

### 5. Use Provider Chain

Combine with other providers for fallback:
```properties
aletheia.providers=AWS,FILE,ENV
```

## Troubleshooting

### Secret Not Found

1. Check secret exists: `aws secretsmanager describe-secret --secret-id my-secret`
2. Verify region matches secret's region
3. Check IAM permissions

### Authentication Errors

1. Verify credentials: `aws sts get-caller-identity`
2. Check credentials file location: `~/.aws/credentials`
3. Verify environment variables are set

### Network Errors

1. Check AWS service status
2. Verify network connectivity
3. Check VPC/Security Group settings if on EC2

## Cost Considerations

AWS Secrets Manager pricing:
- $0.40 per secret per month
- $0.05 per 10,000 API calls

**Minimize costs:**
- Use caching (configure TTL)
- Combine with FILE provider for development
- Cache aggressively in production

## See Also

- [Providers](Providers.md) - General provider information
- [Provider Chain](Provider-Chain.md) - Using AWS with other providers
- [Common Issues](Common-Issues.md) - Troubleshooting

