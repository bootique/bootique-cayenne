package io.bootique.cayenne.jcache.cache;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

// TODO: get rid of this once Cayenne starts supporting untyped caches per CAY-2259
public class TempJCacheConfigurationFactory {

    private final Configuration<Object, Object> configuration = new MutableConfiguration<Object, Object>()
            .setStoreByValue(false)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));

    /**
     * @param cacheGroup is unused by default configuration factory
     * @return cache configuration
     */
    public Configuration<Object, Object> create(String cacheGroup) {
        return configuration;
    }
}
