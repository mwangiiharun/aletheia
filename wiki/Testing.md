# Testing

Testing strategies and best practices for applications using Aletheia.

## Unit Testing

### Mock Secrets with System Properties

```java
import org.junit.jupiter.api.*;

class MyServiceTest {
    
    @BeforeEach
    void setup() {
        System.setProperty("TEST_SECRET", "test-value");
        System.setProperty("alethe渐.providers", "ENV");
        Aletheia.reinitializeProviders();
    }
    
    @AfterEach
    void cleanup() {
        System.clearProperty("TEST_SECRET");
        System.clearProperty("aletheia.providers");
    }
    
    @Test
    void testWithSecret() throws Exception {
        String secret = Aletheia.getSecret("TEST_SECRET");
        assertEquals("test-value", secret);
    }
}
```

### Use Temporary Files

```java
import java.nio.file.Files;
import java.nio.file.Path;

class FileProviderTest {
    private Path tempFile;
    
    @BeforeEach
    void setup() throws Exception {
        tempFile = Files.createTempFile("secrets", ".json");
        String json = "{ \"TEST_SECRET\": \"test-value\" }";
        Files.write(tempFile, json.getBytes());
        
        System.setProperty("file.path", tempFile.toString());
        System.setProperty("aletheia.providers", "FILE");
        Aletheia.reinitializeProviders();
    }
    
    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(tempFile);
        System.clearProperty("file.path");
    }
    
    @Test
    void testSecretFromFile() throws Exception {
        String secret = Aletheia.getSecret("TEST_SECRET");
        assertEquals("test-value", secret);
    }
}
```

## Mock Providers

### Create a Mock Provider

```java
import io.github.mwangiiharun.aletheia.SecretProvider;
import io.github.mwangiiharun.aletheia.providers.CachedProvider;
import java.util.HashMap;
import java.util.Map;

class MockSecretProvider implements SecretProvider {
    private final Map<String, String> secrets = new HashMap<>();
    
    public void addSecret(String key, String value) {
        secrets.put(key, value);
    }
    
    @Override
    public String getSecret(String key) {
        return secrets.get(key);
    }
    
    @Override
    public boolean supports(String key) {
        return secrets.containsKey(key);
    }
    
    @Override
    public void close() {
        secrets.clear();
    }
}
```

### Use Mock Provider in Tests

```java
import io.github.mwangiiharun.aletheia.Aletheia;

class MyServiceTest {
    
    @BeforeEach
    void setup() {
        MockSecretProvider mockProvider = new MockSecretProvider();
        mockProvider.addSecret("TEST_SECRET", "mock-value");
        
        Aletheia.setProvidersForTesting(
            List.of(new CachedProvider(mockProvider, 3600))
        );
    }
    
    @Test
    void testWithMockProvider() throws Exception {
        String secret = Aletheia.getSecret("TEST_SECRET");
        assertEquals("mock-value", secret);
    }
}
```

## Spring Boot Testing

### Test with TestPropertySource

```java
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "aletheia.providers=ENV",
    "TEST_SECRET=test-value"
})
class SpringBootTest {
    
    @Test
    void testService() {
        // Service will have secrets injected
    }
}
```

### Use ApplicationContextInitializer

```java
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class TestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        System.setProperty("TEST_SECRET", "test-value");
        Aletheia.reinitializeProviders();
    }
}
```

## Integration Testing

### Test Provider Chain

```java
class ProviderChainTest {
    
    @Test
    void testProviderFallback() throws Exception {
        // Setup: First provider has no secret, second has it
        System.setProperty("aletheia.providers", "ENV,FILE");
        
        // Create file with secret
        Path file = Files.createTempFile("secrets", ".json");
        Files.write(file, "{ \"SECRET\": \"from-file\" }".getBytes());
        System.setProperty("file.path", file.toString());
        
        Aletheia.reinitializeProviders();
        
        // ENV doesn't have it, should fallback to FILE
        String secret = Aletheia.getSecret("SECRET");
        assertEquals("from-file", secret);
        
        Files.delete(file);
    }
}
```

## Test Best Practices

### Getting Started

1. **Isolate Tests**: Each test should set up its own secrets
2. **Clean Up**: Always clean up system properties after tests
3. **Use Test Prefix**: Prefix test secrets with `TEST_` or `MOCK_`

### Test Structure

```java
class MyServiceTest {
    
    @BeforeEach
    void setup() {
        // Setup test environment
        System.setProperty("TEST_SECRET", "test-value");
        Aletheia.reinitializeProviders();
    }
    
    @AfterEach
    void cleanup() {
        // Clean up
        System.clearProperty("TEST_SECRET");
        Aletheia.reinitializeProviders();
    }
    
    @Test
    void testFeature() {
        // Test code
    }
}
```

### Test Helper Methods

```java
class TestHelper {
    static void set证明材料TestSecret(String key, String value) {
        System.setProperty(key, value);
        Aletheia.reinitializeProviders();
    }
    
    static void clearTestSecret(String key) {
        System.clearProperty(key);
    }
    
    static void setupTestFileProvider(Path file) {
        System.setProperty("file.path", file.toString());
        System.setProperty("aletheia.providers", "FILE");
        Aletheia.reinitializeProviders();
    }
}
```

## Testing Error Scenarios

### Test Missing Secrets

```java
@Test
void testMissingSecret() {
    assertThrows(SecretNotFoundException.class, () -> {
        Aletheia.getSecret("NON_EXISTENT");
    });
}
```

### Test Provider Failures

```java
@Test
void testProviderFailure() {
    // Setup provider that will fail
    System.setProperty("vault.url", "http://invalid:8200");
    System.setProperty("aletheia.providers", "VAULT");
    
    assertThrows(ProviderException.class, () -> {
        Aletheia.getSecret("SECRET");
    });
}
```

## Continuous Integration

### CI/CD Configuration

```yaml
# GitHub Actions example
env:
  TEST_SECRET: test-value-for-ci
  ATHETHEIA_PROVIDERS: ENV
```

### Test Isolation

Ensure tests don't depend on external services:
- Use ENV or FILE providers
- Mock cloud providers
- Use local test fixtures

## See Also

- [Best Practices](Best-Practices.md) - Testing best practices
- [Common Issues](Common-Issues.md) - Troubleshooting test issues

