# Installation

Add Aletheia to your Java project using Maven or Gradle.

## Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.mwangiiharun</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Gradle

Add the following to your `build.gradle`:

```gradle
implementation 'io.github.mwangiiharun:aletheia:0.2.0'
```

Or for Kotlin DSL (`build.gradle.kts`):

```kotlin
implementation("io.github.mwangiiharun:aletheia:0.2.0")
```

## Prerequisites

- **Java 17+** - Aletheia requires Java 17 or higher
- **Maven 3.6+** or **Gradle 6.8+** (for dependency management)

## Optional Dependencies

### Spring Boot

If you're using Spring Boot, Aletheia will automatically configure itself:

```xml
<dependency>
    <groupId>io.github.mwangiiharun</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.2.0</version>
</dependency>
```

### AWS Secrets Manager

If you plan to use AWS Secrets Manager, ensure you have AWS SDK v2 in your classpath (already included if using AWS provider):

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
    <version>2.27.31</version>
</dependency>
```

### Google Cloud Secret Manager

If you plan to use GCP Secret Manager, ensure you have the GCP Secret Manager library:

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-secret-manager</artifactId>
    <version>2.47.0</version>
</dependency>
```

### HashiCorp Vault

Vault provider uses standard HTTP client (no additional dependencies needed).

## Verifying Installation

After adding the dependency, verify it's working:

```java
import io.github.mwangiiharun.aletheia.Aletheia;

public class TestInstallation {
    public static void main(String[] args) {
        System.out.println("Aletheia version: " + 
            Aletheia.class.getPackage().getImplementationVersion());
    }
}
```

## Next Steps

- [Quick Start](Quick-Start.md) - Get started with your first secret
- [Configuration](Configuration.md) - Configure providers and settings

---

📖 [← Back to Documentation Index](README.md) | [← Back to Main README](../README.md)

