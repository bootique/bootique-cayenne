package io.bootique.cayenne.jcache.cache;

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.jcache.JCacheConstants;
import org.apache.cayenne.jcache.JCacheEntryLoader;
import org.apache.cayenne.query.QueryMetadata;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// TODO: get rid of this once Cayenne starts supporting untyped caches per CAY-2259
public class TempJCacheQueryCache implements QueryCache {

    @Inject
    private CacheManager cacheManager;

    @Inject
    private TempJCacheConfigurationFactory configurationFactory;

    private ConcurrentMap<String, Object> seenCacheNames = new ConcurrentHashMap<>();

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
                getCache(cache).remove(key);
            }
        }
    }

    @Override
    public void removeGroup(String groupKey) {
        Cache<String, List> cache = getCache(groupKey);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void clear() {
        for (String name : seenCacheNames.keySet()) {
            getCache(name).clear();
        }
    }

    @Override
    @Deprecated
    public int size() {
        return -1;
    }

    protected Cache<String, List> createIfAbsent(QueryMetadata metadata) {
        return createIfAbsent(cacheName(metadata));
    }

    protected Cache<String, List> createIfAbsent(String cacheName) {

        Cache cache = getCache(cacheName);
        if (cache == null) {

            try {
                cache = cacheManager.createCache(cacheName, configurationFactory.create(cacheName));
            } catch (CacheException e) {
                // someone else just created this cache?
                cache = getCache(cacheName);
                if (cache == null) {
                    // giving up... the error was about something else...
                    throw e;
                }
            }

            seenCacheNames.put(cacheName, 1);
        }

        return cache;
    }

    protected Cache<String, List> getCache(String name) {
        return cacheManager.getCache(name);
    }

    protected String cacheName(QueryMetadata metadata) {

        String cacheGroup = metadata.getCacheGroup();
        if (cacheGroup != null) {
            return cacheGroup;
        }

        // no explicit cache groups
        return JCacheConstants.DEFAULT_CACHE_NAME;
    }

    @BeforeScopeEnd
    public void shutdown() {
        cacheManager.close();
    }
}
