package com.aletheia.providers;

import com.aletheia.SecretProvider;
import com.aletheia.exceptions.ProviderException;

import java.util.concurrent.ConcurrentHashMap;

public class CachedProvider implements SecretProvider {
    private final SecretProvider delegate;
    private final long ttlMillis;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(String value, long expiresAt) {}

    public CachedProvider(SecretProvider delegate, long ttlSeconds) {
        this.delegate = delegate;
        this.ttlMillis = ttlSeconds * 1000;
    }

    @Override
    public String getSecret(String key) throws ProviderException {
        CacheEntry entry = cache.get(key);
        long now = System.currentTimeMillis();

        if (entry != null && entry.expiresAt() > now)
            return entry.value();

        String value = delegate.getSecret(key);
        if (value != null) {
            cache.put(key, new CacheEntry(value, now + ttlMillis));
        }
        return value;
    }

    @Override
    public boolean supports(String key) {
        return delegate.supports(key);
    }
}