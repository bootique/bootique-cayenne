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

package io.bootique.cayenne;

import io.bootique.ConfigModule;
import io.bootique.cayenne.annotation.CayenneConfigs;
import io.bootique.cayenne.annotation.CayenneListener;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.log.BootLogger;
import io.bootique.shutdown.ShutdownManager;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import javax.inject.Singleton;


public class CayenneModule extends ConfigModule {

    public CayenneModule() {
    }

    public CayenneModule(String configPrefix) {
        super(configPrefix);
    }

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneModuleExtender} that can be used to load Cayenne custom extensions.
     * @since 0.19
     */
    public static CayenneModuleExtender extend(Binder binder) {
        return new CayenneModuleExtender(binder);
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
        return config(ServerRuntimeFactory.class, configFactory);
    }

    @Provides
    @Singleton
    protected ServerRuntime createCayenneRuntime(
            ServerRuntimeFactory serverRuntimeFactory,
            DataSourceFactory dataSourceFactory,
            BootLogger bootLogger,
            ShutdownManager shutdownManager,
            Set<Module> customModules,
            @CayenneListener Set<Object> listeners,
            Set<DataChannelFilter> filters,
            CayenneConfigMerger configMerger,
            @CayenneConfigs Set<String> injectedCayenneConfigs) {

        Collection<Module> extraModules = extraCayenneModules(customModules, filters);

        ServerRuntime runtime = serverRuntimeFactory.createCayenneRuntime(
                dataSourceFactory,
                configMerger,
                extraModules,
                injectedCayenneConfigs);

        shutdownManager.addShutdownHook(() -> {
            bootLogger.trace(() -> "shutting down Cayenne...");
            runtime.shutdown();
        });

        // TODO: listeners should be really contributable to Cayenne via DI, just like filters...
        if (!listeners.isEmpty()) {
            DataDomain domain = runtime.getDataDomain();
            listeners.forEach(domain::addListener);
        }

        return runtime;
    }

    protected Collection<Module> extraCayenneModules(Set<Module> customModules, Set<DataChannelFilter> filters) {
        Collection<Module> extras = new ArrayList<>(customModules);

        if (!filters.isEmpty()) {
            extras.add(cayenneBinder -> {
                ListBuilder<DataChannelFilter> listBinder = ServerModule.contributeDomainFilters(cayenneBinder);
                filters.forEach(listBinder::add);
            });
        }

        return extras;
    }
}
