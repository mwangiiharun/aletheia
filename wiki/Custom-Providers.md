# Custom Providers

Create custom secret providers by implementing the `SecretProvider` interface.

## SecretProvider Interface

```java
public interface SecretProvider extends AutoCloseable {
    String getSecret(String key) throws ProviderException;
    boolean supports(String key);
    void close() throws Exception;
}
```

## Basic Custom Provider

### Step 1: Implement the Interface

```java
import io.github.mwangiiharun.aletheia.SecretProvider;
import io.github.mwangiiharun.aletheia.exceptions.ProviderException;

public class MyCustomProvider implements SecretProvider {
    
    @Override
    public String getSecret(String key) throws ProviderException {
        // Your secret retrieval logic
        if (key.equals("MY_SECRET")) {
            return "secret-value";
        }
        return null;  // Return null if not found
    }
    
    @Override
    public boolean supports(String key) {
        // Return true if this provider can handle this key
        return key.startsWith("MY_");
    }
    
    @Override
   16
    public void close() throws Exception {
        // Cleanup resources if needed
    }
}
```

### Step 2: Use Your Provider

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.providers.CachedProvider;

// Create and wrap in CachedProvider
MyCustomProvider customProvider = new MyCustomProvider();
CachedProvider cachedProvider = new CachedProvider(customProvider, 3600);

// Set as provider
Aletheia.setProvidersForTesting(List.of(cachedProvider));

// Use
String secret = Aletheia.getSecret("MY_SECRET");
```

## Complete Example: Database Provider

```java
import io.github.mwangiiharun.aletheia.SecretProvider;
import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import java.sql.*;

public class DatabaseSecretProvider implements SecretProvider {
    
    private final Connection connection;
    
    public DatabaseSecretProvider(String jdbcUrl, String username, String password) 
            throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, username, password);
    }
    
    @Override
    public String getSecret(String key) throws ProviderException {
        try {
            String sql = "SELECT value FROM secrets WHERE key = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, key);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("value");
                    }
                }
            }
            return null;  // Not found
        } catch (SQLException e) {
            throw new ProviderException("Database", e);
        }
    }
    
    @Override
    public boolean supports(String key) {
        // Support all keys (or filter by prefix if needed)
        return true;
    }
    
    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
```

## Example: HTTP API Provider

```java
import io.github.mwangiiharun.aletheia.SecretProvider;
import io.github.mwangiiharun.aletheia.exceptions.ProviderException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpApiProvider implements SecretProvider {
    
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    
    public HttpApiProvider(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }
    
    @Override
    public String getSecret(String key) throws ProviderException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/secrets/" + key))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                // Parse JSON response
                // For simplicity, assuming plain text
                return response.body();
            } else if (response.statusCode() == 404) {
                return null;  // Not found
            } else {
                throw new ProviderException("HTTP", 
                    new RuntimeException("HTTP " + response.statusCode()));
            }
        } catch (Exception e) {
            throw new ProviderException("HTTP", e);
        }
    }
    
    @Override
    public boolean supports(String key) {
        return key.startsWith("API_");
    }
    
    @Override
    public void close() throws Exception {
        // HTTP client cleanup if needed
    }
}
```

## Using Custom Providers

### Method 1: Direct Registration

```java
import io.github.mwangiiharun.aletheia.Aletheia;
import io.github.mwangiiharun.aletheia.providers.CachedProvider;
import java.util.List;

// Create custom provider
MyCustomProvider customProvider = new MyCustomProvider();

// Wrap in CachedProvider (recommended)
CachedProvider cached = new CachedProvider(customProvider, 3600);

// Register
Aletheia.setProvidersForTesting(List.of(cached));

// Use
String secret = Aletheia.getSecret("MY_SECRET");
```

### Method 2: With Other Providers

```java
import io.github.mwangiiharun.aletheia.providers.*;

// Create multiple providers
EnvProvider envProvider = new EnvProvider();
MyCustomProvider customProvider = new MyCustomProvider();
FileProvider fileProvider = new FileProvider();

// Wrap and combine
List<SecretProvider> providers = List.of(
    new CachedProvider(customProvider, 3600),
    new CachedProvider(envProvider, 3600),
    new CachedProvider(fileProvider, 3600)
);

Aletheia.setProvidersForTesting(providers);
```

## Best Practices

### 1. Always Wrap in CachedProvider

```java
// ✅ Good
CachedProvider cached = new CachedProvider(myProvider, 3600);

// ❌ Bad - No caching
Aletheia.setProvidersForTesting(List.of(myProvider));
```

### 2. Implement supports() Correctly

```java
@Override
public boolean supports(String key) {
    // Return true only if this provider can handle the key
    return key.startsWith("MY_PREFIX_");
}
```

### 3. Return null for Not Found

```java
@Override
public String getSecret(String key) throws ProviderException {
    // Return null if secret doesn't exist
    // Don't throw exception unless there's an error
    if (secretExists(key)) {
        return retrieveSecret(key);
    }
    return null;  // Let next provider try
}
```

### 4. Throw ProviderException on Errors

```java
@Override
public String getSecret(String key) throws ProviderException {
    try {
        // Your logic
    } catch (Exception e) {
        throw new ProviderException("MyProvider", e);
    }
}
```

### 5. Implement Proper Cleanup

```java
@Override
public void close() throws Exception {
    // Close connections, streams, etc.
    if (connection != null) {
        connection.close();
    }
}
```

## Configuration-Based Provider

Create a provider that reads from configuration:

```java
public class ConfigBasedProvider implements SecretProvider {
    
    private final Properties config;
    
    public ConfigBasedProvider(Properties config) {
        this.config = config;
    }
    
    @Override
    public String getSecret(String key) {
        return config.getProperty(key);
    }
    
    @Override
    public boolean supports(String key) {
        return config.containsKey(key);
    }
    
    @Override
    public void close() {
        // No cleanup needed
    }
}
```

## Testing Custom Providers

```java
public class MyCustomProviderTest {
    
    @Test
    void testGetSecret() throws Exception {
        MyCustomProvider provider = new MyCustomProvider();
        
        String secret = provider.getSecret("MY_SECRET");
        assertEquals("expected-value", secret);
        
        String notFound = provider.getSecret("NON_EXISTENT");
        assertNull(notFound);
    }
    
    @Test
    void testSupports() {
        MyCustomProvider provider = new MyCustomProvider();
        
        assertTrue(provider.supports("MY_SECRET"));
        assertFalse(provider.supports("OTHER_SECRET"));
    }
}
```

## See Also

- [Providers](Providers.md) - Understanding built-in providers
- [Provider Chain](Provider-Chain.md) - How providers work together
- [Best Practices](Best-Practices.md) - General best practices

