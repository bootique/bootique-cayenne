package io.bootique.cayenne.jcache;

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.query.QueryMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;

// a candidate for inclusion on Cayenne...
public class JCacheQueryCache implements QueryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCacheQueryCache.class);

    /**
     * Default JCache cache name. This will be the cache used for queries with no explicit cache groups.
     */
    public static final String DEFAULT_CACHE_NAME = "cayenne.default.cache";

    private CacheManager cacheManager;
    private Configuration<String, List> newCachesConfig;
    private ConcurrentMap<String, Object> seenCacheNames;

    public JCacheQueryCache(CacheManager cacheManager, Configuration<String, List> newCachesConfig) {
        this.cacheManager = cacheManager;
        this.newCachesConfig = newCachesConfig;
        this.seenCacheNames = new ConcurrentHashMap<>();
    }

    @Override
    public List get(QueryMetadata metadata) {
        String key = Objects.requireNonNull(metadata.getCacheKey());
        Cache<String, List> cache = createIfAbsent(metadata);

        return cache.get(key);
    }

    @Override
    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        String key = Objects.requireNonNull(metadata.getCacheKey());
        Cache<String, List> cache = createIfAbsent(metadata);

        List<?> result = cache.get(key);
        return result != null
                ? result
                : cache.invoke(key, new JCacheEntryLoader(factory));
    }

    @Override
    public void put(QueryMetadata metadata, List results) {
        String key = Objects.requireNonNull(metadata.getCacheKey());
        Cache<String, List> cache = createIfAbsent(metadata);

        cache.put(key, results);
    }

    @Override
    public void remove(String key) {
        if (key != null) {
            for (String cache : cacheManager.getCacheNames()) {
                cacheManager.getCache(cache).remove(key);
            }
        }
    }

    @Override
    public void removeGroup(String groupKey) {
        Cache<?, ?> cache = cacheManager.getCache(groupKey, String.class, List.class);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void clear() {
        seenCacheNames.keySet().forEach(name -> cacheManager.getCache(name, String.class, List.class).clear());
    }

    /**
     * Returns -1 to indicate that we can't calculate the size. JCache and EhCache can potentially have a complex topology
     * that can not be meaningfully described by a single int. Use other means (like provider-specific JMX) to monitor cache.
     *
     * @return -1
     */
    @Override
    public int size() {
        return -1;
    }

    protected Cache<String, List> createIfAbsent(QueryMetadata metadata) {
        return createIfAbsent(cacheName(metadata));
    }

    protected Cache<String, List> createIfAbsent(String cacheName) {

        Cache<String, List> cache = cacheManager.getCache(cacheName, String.class, List.class);
        if (cache == null) {

            try {
                cache = cacheManager.createCache(cacheName, newCachesConfig);
            } catch (CacheException e) {
                // someone else just created this cache?
                cache = cacheManager.getCache(cacheName, String.class, List.class);
                if (cache == null) {
                    // giving up... the error was about something else...
                    throw e;
                }
            }

            seenCacheNames.put(cacheName, 1);
        }

        return cache;
    }

    protected String cacheName(QueryMetadata metadata) {

        String[] cacheGroups = metadata.getCacheGroups();

        if (cacheGroups != null && cacheGroups.length > 0) {

            if (cacheGroups.length > 1) {
                if (LOGGER.isWarnEnabled()) {
                    List<String> ignored = asList(cacheGroups).subList(1, cacheGroups.length);
                    LOGGER.warn("multiple cache groups per key '" + metadata.getCacheKey() + "', using the first one: "
                            + cacheGroups[0] + ". Ignoring others: " + ignored);
                }
            }

            return cacheGroups[0];
        }

        // no explicit cache groups
        return DEFAULT_CACHE_NAME;
    }

    protected Cache<?, ?> forGroupOrDefault(String group) {
        Cache<?, ?> cache = cacheManager.getCache(group);
        return cache != null ? cache : Objects.requireNonNull(cacheManager.getCache(DEFAULT_CACHE_NAME));
    }
}
