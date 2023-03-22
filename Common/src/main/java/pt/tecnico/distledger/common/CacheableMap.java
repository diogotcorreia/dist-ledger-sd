package pt.tecnico.distledger.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class CacheableMap<K, V> {

    private final Map<K, CacheEntry<V>> map = new ConcurrentHashMap<>();

    private final long ttl;

    public CacheableMap(long ttl, TimeUnit unit) {
        this.ttl = unit.toMillis(ttl);
    }

    private long calculateExpiryTime() {
        return System.currentTimeMillis() + this.ttl;
    }

    public synchronized void put(K key, V value) {
        this.map.put(key, new CacheEntry<>(value, calculateExpiryTime()));
    }

    public synchronized Optional<V> get(K key) {
        return Optional.ofNullable(this.map.get(key))
                .filter(CacheEntry::isValid)
                .map(CacheEntry::value);
    }

    public synchronized void remove(K key) {
        this.map.remove(key);
    }

    private synchronized void clearExpiredEntries() {
        this.map.values().removeIf(CacheEntry::isExpired);
    }

    public synchronized int size() {
        clearExpiredEntries();
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public Stream<V> streamValues() {
        return this.map.values().stream().map(CacheEntry::value);
    }

    public record CacheEntry<T>(T value, long expiryTime) {
        public boolean isValid() {
            return System.currentTimeMillis() < expiryTime;
        }

        public boolean isExpired() {
            return !isValid();
        }
    }

}
