/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.cayenne.v50.jcache;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.cayenne.v50.CayenneModule;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.Provides;
import org.apache.cayenne.cache.invalidation.CacheInvalidationModule;
import org.apache.cayenne.cache.invalidation.CacheInvalidationModuleExtender;
import org.apache.cayenne.cache.invalidation.InvalidationHandler;

import javax.cache.CacheManager;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Bootique DI module integrating bootique-jcache to Cayenne.
 */
public class CayenneJCacheModule implements BQModule {

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneJCacheModuleExtender} that can be used to load Cayenne cache
     * custom extensions.
     */
    public static CayenneJCacheModuleExtender extend(Binder binder) {
        return new CayenneJCacheModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Apache Cayenne 4.2 JCache extensions")
                .build();
    }

    @Override
    public void configure(Binder binder) {
        extend(binder).initAllExtensions();

        CayenneModule.extend(binder).addModule(Key.get(org.apache.cayenne.di.Module.class, DefinedInCayenneJCache.class));
    }

    @Singleton
    @Provides
    @DefinedInCayenneJCache
    org.apache.cayenne.di.Module provideDiJCacheModule(CacheManager cacheManager, Set<InvalidationHandler> invalidationHandlers) {
        // return module composition
        return b -> {
            createInvalidationModule(invalidationHandlers).configure(b);
            createOverridesModule(cacheManager).configure(b);
        };
    }

    protected org.apache.cayenne.di.Module createInvalidationModule(Set<InvalidationHandler> invalidationHandlers) {
        return b -> {
            CacheInvalidationModuleExtender extender = CacheInvalidationModule.extend(b);
            invalidationHandlers.forEach(extender::addHandler);
        };
    }

    protected org.apache.cayenne.di.Module createOverridesModule(CacheManager cacheManager) {
        return b -> b.bind(CacheManager.class).toInstance(cacheManager);
    }

    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface DefinedInCayenneJCache {
    }
}
