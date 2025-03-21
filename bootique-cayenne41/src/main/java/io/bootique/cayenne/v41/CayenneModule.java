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

package io.bootique.cayenne.v41;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.cayenne.v41.annotation.CayenneConfigs;
import io.bootique.cayenne.v41.annotation.CayenneListener;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.shutdown.ShutdownManager;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @deprecated the users are encouraged to switch to Cayenne 4.2
 */
@Deprecated(since = "3.0", forRemoval = true)
public class CayenneModule implements BQModule {

    private static final String CONFIG_PREFIX = "cayenne";

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneModuleExtender} that can be used to load Cayenne custom extensions.
     */
    public static CayenneModuleExtender extend(Binder binder) {
        return new CayenneModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Deprecated, can be replaced with 'bootique-cayenne42'.")
                .config(CONFIG_PREFIX, ServerRuntimeFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
        extend(binder).initAllExtensions();
    }

    @Provides
    @Singleton
    CayenneConfigMerger provideConfigMerger() {
        return new CayenneConfigMerger();
    }

    @Provides
    @Singleton
    ServerRuntimeFactory createServerRuntimeFactory(ConfigurationFactory configFactory) {
        return configFactory.config(ServerRuntimeFactory.class, CONFIG_PREFIX);
    }

    @Provides
    @Singleton
    protected ServerRuntime createCayenneRuntime(
            ServerRuntimeFactory serverRuntimeFactory,
            DataSourceFactory dataSourceFactory,
            ShutdownManager shutdownManager,
            Set<Module> customModules,
            @CayenneListener Set<Object> listeners,
            Set<DataChannelFilter> filters,
            Set<DataChannelQueryFilter> queryFilters,
            Set<DataChannelSyncFilter> syncFilters,
            CayenneConfigMerger configMerger,
            @CayenneConfigs Set<String> injectedCayenneConfigs,
            Set<CayenneStartupListener> startupCallbacks) {

        Collection<Module> extras = extraCayenneModules(customModules, filters, queryFilters, syncFilters);
        ServerRuntime runtime = serverRuntimeFactory.createCayenneRuntime(
                dataSourceFactory,
                configMerger,
                extras,
                injectedCayenneConfigs);

        shutdownManager.onShutdown(runtime, ServerRuntime::shutdown);

        // TODO: listeners should be wrapped in a CayenneModule and added to Cayenne via DI, just like filters...
        if (!listeners.isEmpty()) {
            DataDomain domain = runtime.getDataDomain();
            listeners.forEach(domain::addListener);
        }

        startupCallbacks.forEach(c -> c.onRuntimeCreated(runtime));

        return runtime;
    }

    protected Collection<Module> extraCayenneModules(
            Set<Module> customModules,
            Set<DataChannelFilter> filters,
            Set<DataChannelQueryFilter> queryFilters,
            Set<DataChannelSyncFilter> syncFilters) {

        Collection<Module> extras = new ArrayList<>(customModules);

        if (!filters.isEmpty()) {
            extras.add(cayenneBinder -> {
                ListBuilder<DataChannelFilter> listBinder = ServerModule.contributeDomainFilters(cayenneBinder);
                filters.forEach(listBinder::add);
            });
        }

        if (!queryFilters.isEmpty()) {
            extras.add(cayenneBinder -> {
                ListBuilder<DataChannelQueryFilter> listBinder = ServerModule.contributeDomainQueryFilters(cayenneBinder);
                queryFilters.forEach(listBinder::add);
            });
        }

        if (!syncFilters.isEmpty()) {
            extras.add(cayenneBinder -> {
                ListBuilder<DataChannelSyncFilter> listBinder = ServerModule.contributeDomainSyncFilters(cayenneBinder);
                syncFilters.forEach(listBinder::add);
            });
        }

        return extras;
    }
}
