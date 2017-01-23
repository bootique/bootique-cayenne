package io.bootique.cayenne.jcache;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.cayenne.CayenneModule;
import io.bootique.cayenne.jcache.invalidation.CacheGroupsHandler;
import io.bootique.cayenne.jcache.invalidation.InvalidationHandler;
import io.bootique.jcache.JCacheModule;

import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.List;
import java.util.Set;

/**
 * Bootique DI module integrating bootique-jcache to Cayenne.
 */
public class CayenneJCacheModule implements Module {

    public static Multibinder<InvalidationHandler> contributeInvalidationHandler(Binder binder) {
        return Multibinder.newSetBinder(binder, InvalidationHandler.class);
    }

    public static LinkedBindingBuilder<Configuration<?, ?>> contributeDefaultCacheConfiguration(Binder binder) {
        return JCacheModule.contributeConfiguration(binder).addBinding(JCacheQueryCache.DEFAULT_CACHE_NAME);
    }

    @Override
    public void configure(Binder binder) {

        // always support a handler for @CacheGroups annotation.
        CayenneJCacheModule.contributeInvalidationHandler(binder).addBinding().to(CacheGroupsHandler.class);

        CayenneModule.contributeModules(binder).addBinding().to(CayenneDIJCacheModule.class);
    }

    @Singleton
    @Provides
    CayenneDIJCacheModule provideDiEhCacheModule(JCacheQueryCache queryCache, Set<InvalidationHandler> invalidationHandlers) {
        return new CayenneDIJCacheModule(queryCache, invalidationHandlers);
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
