package io.bootique.cayenne.jcache;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ModuleExtender;
import io.bootique.jcache.JCacheModule;
import org.apache.cayenne.cache.invalidation.InvalidationHandler;
import org.apache.cayenne.jcache.JCacheConstants;

import javax.cache.configuration.Configuration;

/**
 * @since 0.19
 */
public class CayenneJCacheModuleExtender extends ModuleExtender<CayenneJCacheModuleExtender> {

    public CayenneJCacheModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneJCacheModuleExtender initAllExtensions() {
        contributeInvalidationHandler();
        return this;
    }

    public CayenneJCacheModuleExtender addInvalidationHandler(InvalidationHandler handler) {
        contributeInvalidationHandler().addBinding().toInstance(handler);
        return this;
    }

    public CayenneJCacheModuleExtender addInvalidationHandler(Class<? extends InvalidationHandler> handlerType) {
        contributeInvalidationHandler().addBinding().to(handlerType);
        return this;
    }

    // TODO: we actually know key and value types for Cayenne QueryCache config
    public CayenneJCacheModuleExtender setDefaultCacheConfiguration(Configuration<?, ?> config) {
        JCacheModule.extend(binder).setConfiguration(JCacheConstants.DEFAULT_CACHE_NAME, config);
        return this;
    }

    protected Multibinder<InvalidationHandler> contributeInvalidationHandler() {
        return newSet(InvalidationHandler.class);
    }
}
