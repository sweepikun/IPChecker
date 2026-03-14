package cn.popcraft.ipchecker.Services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerCheckCache {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttl;

    private static class CacheEntry {
        private final String ip;
        private final boolean isClean;
        private final String checkType;
        private final long timestamp;

        CacheEntry(String ip, boolean isClean, String checkType) {
            this.ip = ip;
            this.isClean = isClean;
            this.checkType = checkType;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttl) {
            return System.currentTimeMillis() - timestamp > ttl;
        }
    }

    public PlayerCheckCache(long ttlMinutes) {
        this.ttl = TimeUnit.MINUTES.toMillis(ttlMinutes);
    }

    public void put(String playerName, String ip, boolean isClean, String checkType) {
        cache.put(playerName.toLowerCase(), new CacheEntry(ip, isClean, checkType));
    }

    public CacheEntry get(String playerName) {
        CacheEntry entry = cache.get(playerName.toLowerCase());
        if (entry == null || entry.isExpired(ttl)) {
            cache.remove(playerName.toLowerCase());
            return null;
        }
        return entry;
    }

    public boolean isChecked(String playerName) {
        return get(playerName) != null;
    }

    public boolean isClean(String playerName) {
        CacheEntry entry = get(playerName);
        return entry != null && entry.isClean;
    }

    public String getCachedIP(String playerName) {
        CacheEntry entry = get(playerName);
        return entry != null ? entry.ip : null;
    }

    public void invalidate(String playerName) {
        cache.remove(playerName.toLowerCase());
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
