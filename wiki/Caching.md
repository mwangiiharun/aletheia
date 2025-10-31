# Caching

Aletheia uses TTL-based caching to reduce provider calls and improve performance.

## How Caching Works

All providers are automatically wrapped in a `CachedProvider` that:
1. Caches secret values in memory
2. Returns cached values until TTL expires
3. Refreshes from provider when cache expires

## Configuration

Configure cache TTL in seconds:

```properties
aletheia.cache.ttl.seconds=3600
```

**Default**: 3600 seconds (1 hour)

## Cache Behavior

### Cache Hit
```
Request Secret → Check Cache → Found & Valid → Return Cached Value
```

### Cache Miss
```
Request Secret → Check Cache → Not Found or Expired → Query Provider → Cache Result → Return Value
```

## TTL Considerations

### Short TTL (Recommended for Frequent Changes)

```properties
# 5 minutes
aletheia.cache.ttl.seconds=300
```

**Use when:**
- Secrets change frequently
- Need near-real-time updates
- Acceptable to make more provider calls

### Long TTL (Recommended for Stable Secrets)

```properties
# 24 hours
aletheia.cache.ttl.seconds=86400
```

**Use when:**
- Secrets rarely change
- Provider calls are expensive
- Performance is critical

### Disable Caching (Not Recommended)

```properties
# 0 = no caching
aletheia.cache.ttl.seconds=0
```

**⚠️ Warning**: Disabling cache may impact performance significantly.

## Cache Invalidation

### Manual Cache Clear

```java
import io.github.mwangiiharun.aletheia.Aletheia;

// Reinitialize providers (clears all caches)
Aletheia.reinitializeProviders();
```

### Automatic Expiration

Cache entries automatically expire after TTL and are refreshed on next access.

## Per-Provider Caching

All providers are wrapped in `CachedProvider` with the same TTL. Individual provider caching behavior:

- **ENV Provider**: Very fast, caching has minimal impact
- **FILE Provider**: Fast (local file), caching reduces file I/O
- **Cloud Providers**: Significant benefit, caching reduces network calls and API costs

## Cache Storage

- **Thread-safe**: Cache is shared across all threads
- **In-memory**: Cached in JVM heap
- **Per-application**: Each application instance has its own cache

## Best Practices

### 1. Balance TTL with Update Frequency

```properties
# Secrets rotate daily
aletheia.cache.ttl.seconds=43200  # 12 hours
```

### 2. Use Appropriate TTL for Environment

**Development:**
```properties
aletheia.cache.ttl.seconds=60  # 1 minute for testing
```

**Production:**
```properties
aletheia.cache.ttl.seconds=3600  # 1 hour
```

### 3. Monitor Cache Performance

Enable debug logging to see cache behavior:

```properties
logging.level.io.github.mwangiiharun.aletheia=DEBUG
```

## Cache Metrics (Future)

Future versions may include:
- Cache hit/miss ratios
- Cache size metrics
- Per-provider cache statistics

## Troubleshooting

### Stale Secrets

If secrets appear stale:
1. Check TTL configuration
2. Reduce TTL if needed
3. Manually clear cache: `Aletheia.reinitializeProviders()`

### High Memory Usage

If cache uses too much memory:
1. Reduce TTL (fewer cached entries)
2. Limit number of secrets
3. Restart application periodically

 Киев
