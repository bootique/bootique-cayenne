package io.bootique.cayenne.ehcache;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.cayenne.CayenneModule;
import io.bootique.cayenne.ehcache.jcache.JCacheQueryCache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.List;

public class CayenneEhCacheModule implements Module {

    @Override
    public void configure(Binder binder) {
        CayenneModule.contributeModules(binder).addBinding().to(CayenneDIEhCacheModule.class);
    }

    @Singleton
    @Provides
    CayenneDIEhCacheModule provideDiEhCacheModule(JCacheQueryCache queryCache) {
        return new CayenneDIEhCacheModule(queryCache);
    }

    @Singleton
    @Provides
    JCacheQueryCache provideQueryCache(CacheManager cacheManager) {

        MutableConfiguration<String, List> configuration =
                new MutableConfiguration<String, List>()
                        .setTypes(String.class, List.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));

        return new JCacheQueryCache(cacheManager, configuration);
    }
}
