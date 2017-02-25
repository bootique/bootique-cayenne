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
 *
 * @since 0.18
 */
public class CayenneJCacheModule implements Module {

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneJCacheModuleExtender} that can be used to load Cayenne cache
     * custom extensions.
     * @since 0.19
     */
    public static CayenneJCacheModuleExtender extend(Binder binder) {
        return new CayenneJCacheModuleExtender(binder);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return a {@link Multibinder} for invalidation handlers.
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneJCacheModuleExtender#addInvalidationHandler(Class)} or similar methods.
     */
    @Deprecated
    public static Multibinder<InvalidationHandler> contributeInvalidationHandler(Binder binder) {
        return Multibinder.newSetBinder(binder, InvalidationHandler.class);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return a {@link Multibinder} for cache configurations.
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneJCacheModuleExtender#setDefaultCacheConfiguration(Configuration)}.
     */
    @Deprecated
    public static LinkedBindingBuilder<Configuration<?, ?>> contributeDefaultCacheConfiguration(Binder binder) {
        return JCacheModule.contributeConfiguration(binder).addBinding(JCacheQueryCache.DEFAULT_CACHE_NAME);
    }

    @Override
    public void configure(Binder binder) {
        CayenneJCacheModule.extend(binder).initAllExtensions()
                // always support a handler for @CacheGroups annotation.
                .addInvalidationHandler(CacheGroupsHandler.class);

        CayenneModule.extend(binder).addModule(CayenneDIJCacheModule.class);
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
