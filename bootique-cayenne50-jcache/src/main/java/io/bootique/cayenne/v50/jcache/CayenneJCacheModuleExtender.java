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

import io.bootique.ModuleExtender;
import io.bootique.di.Binder;
import io.bootique.di.SetBuilder;
import io.bootique.jcache.JCacheModule;
import org.apache.cayenne.cache.invalidation.InvalidationHandler;
import org.apache.cayenne.jcache.JCacheConstants;

import javax.cache.configuration.Configuration;

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
        contributeInvalidationHandler().addInstance(handler);
        return this;
    }

    public CayenneJCacheModuleExtender addInvalidationHandler(Class<? extends InvalidationHandler> handlerType) {
        contributeInvalidationHandler().add(handlerType);
        return this;
    }

    // TODO: we actually know key and value types for Cayenne QueryCache config
    public CayenneJCacheModuleExtender setDefaultCacheConfiguration(Configuration<?, ?> config) {
        JCacheModule.extend(binder).setConfiguration(JCacheConstants.DEFAULT_CACHE_NAME, config);
        return this;
    }

    protected SetBuilder<InvalidationHandler> contributeInvalidationHandler() {
        return newSet(InvalidationHandler.class);
    }
}
