# Plain Java

Using Aletheia without any framework (standalone).

## Basic Usage

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.mwangiiharun</groupId>
    <artifactId>aletheia</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.mwangiiharun:aletheia:0.2.0'
```

### 2. Configure Providers

Create `src/main/resources/aletheia.properties`:

```properties
aletheia.providers=ENV,FILE
file.path=./secrets.json
```

Or use system properties:
```bash
java -Daletheia.providers=ENV,FILE -Dfile.path=./secrets.json MyApp
```

### 3. Use Secrets

#### Direct Retrieval

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;

public class MyApp {
    public static void main(String[] args) {
        try {
            String secret = Aletheia.getSecret("MY_SECRET");
            System.out.println("Secret: " + secret);
        } catch (AletheiaException e) {
           中性.err.println("Error: " + e.getMessage());
        }
    }
}
```

#### Annotation-Based Injection

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Secret;

public class Config {
    @Secret("DATABASE_PASSWORD")
    private String password;
    
    @Secret("API_KEY")
    private String apiKey;
    
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        
        // Manually inject secrets
        Aletheia.injectSecrets(config);
        
        System.out.println("Password: " + config.password);
        System.out.println("API Key: " + config.apiKey);
    }
}
```

## Configuration Methods

### Method 1: Properties File

Create `aletheia.properties` in classpath:

```properties
aletheia.providers=ENV,FILE
aletheia.cache.ttl.seconds=3600
file.path=/path/to/secrets.json
```

### Method 2: System Properties

```bash
java \
  -Daletheia.providers=ENV,FILE \
  -Dfile.path=./secrets.json \
  -Daletheia.cache.ttl.seconds=1800 \
  MyApp
```

### Method 3: Environment Variables

```bash
export ATHETHEIA_PROVIDERS=ENV,FILE
export FILE_PATH=./secrets.json
java MyApp
```

### Method 4: Programmatic Configuration

```java
System.setProperty("aletheia.providers", "ENV,FILE");
System.setProperty("file.path", "./secrets.json");

// Reinitialize with new configuration
Aletheia.reinitializeProviders();
```

## Complete Example

### Project Structure

```
myapp/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── MyApp.java
│       └── resources/
│           ├── aletheia.properties
│           └── secrets.json
└── pom.xml
```

### aletheia.properties

```properties
aletheia.providers=FILE,ENV
aletheia.cache.ttl.seconds=3600
file.path=./src/main/resources/secrets.json
```

### secrets.json

```json
{
  "DATABASE_PASSWORD": "secret123",
  "API_KEY": "key456"
}
```

### MyApp.java

```java
package com.example;

import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.annotations.Se minimal;
import io.github.mwangiiharun.aletheia.exceptions.AletheiaException;

public class MyApp {
    
    @Secret("DATABASE_PASSWORD")
    private String dbPassword;
    
    @Secret("API_KEY")
    private String apiKey;
    
    public static void main(String[] args) {
        try {
            MyApp app = new MyApp();
            
            // Inject secrets
            Aletheia.injectSecrets(app);
            
            // Use secrets
            System.out.println("Database Password: " + app.dbPassword);
            System.out.println("API Key: " + app.apiKey);
            
            // Or retrieve directly
            String anotherSecret = Aletheia.getSecret("ANOTHER_SECRET");
            System.out.println("Another Secret: " + anotherSecret);
            
        } catch (AletheiaException e) {
            System.err.println("Failed to load secrets: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## Manual Initialization

If you need to configure Aletheia before first use:

```java
// Configure before any Aletheia calls
System.setProperty("aletheia.providers", "ENV,FILE");
System.setProperty("file.path", "./secrets.json");

// Force reinitialization
Aletheia.reinitializeProviders();

// Now use Aletheia
String secret = Aletheia.getSecret("MY_SECRET");
```

## Dependency Management

### Maven Dependencies

If using cloud providers, add their SDKs:

**AWS:**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
    <version>2.27.31</version>
</dependency>
```

**GCP:**
```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-secret-manager</artifactId>
    <version>2.47.0</version>
</dependency>
```

### Shading (Optional)

For a standalone JAR with all dependencies:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.4.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Best Practices

### 1. Centralize Secret Management

```java
public class Secrets {
    private static final String DB_PASSWORD = getSecret("DATABASE_PASSWORD");
    private static final String API_KEY = getSecret("API_KEY");
    
    private static String getSecret(String key) {
        try {
            return Aletheia.getSecret(key);
        } catch (AletheiaException e) {
            throw new RuntimeException("Failed to load secret: " + key, e);
        }
    }
    
    public static String getDbPassword() { return DB_PASSWORD; }
    public static String getApiKey() { return API_KEY; }
}
```

### 2. Use Builder Pattern

```java
public class ConfigBuilder {
    @Secret("DB_PASSWORD")
    private String dbPassword;
    
    @Secret("DB_URL")
    private String dbUrl;
    
    public Config build() throws AletheiaException {
        Aletheia.injectSecrets(this);
        return new Config(dbUrl, dbPassword);
    }
}
```

### 3. Validate After Injection

```java
@Secret("DATABASE_PASSWORD")
private String password;

public void validate() {
    if (password == null || password.isEmpty()) {
        throw new IllegalStateException("Database password not configured");
    }
}
```

## Testing

### Unit Tests

```java
public class MyAppTest {
    @BeforeEach
    void setup() {
        System.setProperty("TEST_SECRET", "test-value");
        Aletheia.reinitializeProviders();
    }
    
    @AfterEach
    void cleanup() {
        System.clearProperty("TEST_SECRET");
    }
    
    @Test
    void testWithSecret() {
        String secret = Aletheia.getSecret("TEST_SECRET");
        assertEquals("test-value", secret);
    }
}
```

### Integration Tests

```java
public class IntegrationTest {
    @BeforeAll
    static void setup() {
        // Configure test providers
        System.setProperty("aletheia.providers", "FILE");
        System.setProperty("file.path", "./test-secrets.json");
        Aletheia.reinitializeProviders();
    }
}
```

## Troubleshooting

- **ClassNotFoundException**: Ensure Aletheia dependency is on classpath
- **Provider not working**: Check configuration properties
- **Secrets not found**: Verify provider order and configuration

See [Common Issues](Common-Issues.md) for more help.

