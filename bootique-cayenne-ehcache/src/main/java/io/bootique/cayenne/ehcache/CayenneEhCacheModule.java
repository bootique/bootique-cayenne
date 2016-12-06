package io.bootique.cayenne.ehcache;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.bootique.cayenne.CayenneModule;
import io.bootique.cayenne.ehcache.invalidation.CacheGroupsHandler;
import io.bootique.cayenne.ehcache.invalidation.InvalidationHandler;
import io.bootique.cayenne.ehcache.jcache.JCacheQueryCache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.List;
import java.util.Set;

public class CayenneEhCacheModule implements Module {

    public static Multibinder<InvalidationHandler> contributeInvalidationHandler(Binder binder) {
        return Multibinder.newSetBinder(binder, InvalidationHandler.class);
    }

    @Override
    public void configure(Binder binder) {

        // always support a handler for @CacheGroups annotation.
        CayenneEhCacheModule.contributeInvalidationHandler(binder).addBinding().to(CacheGroupsHandler.class);

        CayenneModule.contributeModules(binder).addBinding().to(CayenneDIEhCacheModule.class);
    }

    @Singleton
    @Provides
    CayenneDIEhCacheModule provideDiEhCacheModule(JCacheQueryCache queryCache, Set<InvalidationHandler> invalidationHandlers) {
        return new CayenneDIEhCacheModule(queryCache, invalidationHandlers);
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
