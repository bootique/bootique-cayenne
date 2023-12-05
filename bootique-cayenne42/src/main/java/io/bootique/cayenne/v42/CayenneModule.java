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

package io.bootique.cayenne.v42;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.cayenne.v42.annotation.CayenneConfigs;
import io.bootique.cayenne.v42.annotation.CayenneListener;
import io.bootique.cayenne.v42.commitlog.CommitLogModuleBuilder;
import io.bootique.cayenne.v42.commitlog.MappedCommitLogListener;
import io.bootique.cayenne.v42.commitlog.MappedCommitLogListenerType;
import io.bootique.cayenne.v42.syncfilter.MappedDataChannelSyncFilter;
import io.bootique.cayenne.v42.syncfilter.MappedDataChannelSyncFilterType;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Provides;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.log.BootLogger;
import io.bootique.shutdown.ShutdownManager;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionFilter;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @since 2.0
 */
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
                .description("Integrates Apache Cayenne ORM, v4.2")
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
            Injector injector,
            ServerRuntimeFactory serverRuntimeFactory,
            DataSourceFactory dataSourceFactory,
            BootLogger bootLogger,
            ShutdownManager shutdownManager,
            Set<Module> customModules,
            @CayenneListener Set<Object> listeners,
            Set<DataChannelQueryFilter> queryFilters,
            Set<MappedDataChannelSyncFilter> syncFilters,
            Set<MappedDataChannelSyncFilterType> syncFilterTypes,
            CayenneConfigMerger configMerger,
            @CayenneConfigs Set<String> injectedCayenneConfigs,
            Set<CayenneStartupListener> startupCallbacks,
            Set<MappedCommitLogListener> commitLogListeners,
            Set<MappedCommitLogListenerType> commitLogListenerTypes,
            Set<ExtendedType> extendedTypes,
            Set<ValueObjectType> valueObjectTypes) {

        Collection<Module> extras = new ArrayList<>(customModules);

        appendExtendedTypesModule(extras, extendedTypes);
        appendValueObjectTypesModule(extras, valueObjectTypes);

        appendQueryFiltersModule(extras, queryFilters);
        appendSyncFiltersModule(extras, injector, syncFilters, syncFilterTypes);
        appendCommitLogModules(extras, injector, commitLogListeners, commitLogListenerTypes);

        ServerRuntime runtime = serverRuntimeFactory.createCayenneRuntime(
                dataSourceFactory,
                configMerger,
                extras,
                injectedCayenneConfigs);

        shutdownManager.addShutdownHook(() -> {
            bootLogger.trace(() -> "shutting down Cayenne...");
            runtime.shutdown();
        });

        // TODO: listeners should be wrapped in a CayenneModule and added to Cayenne via DI, just like filters...
        if (!listeners.isEmpty()) {
            DataDomain domain = runtime.getDataDomain();
            listeners.forEach(domain::addListener);
        }

        startupCallbacks.forEach(c -> c.onRuntimeCreated(runtime));

        return runtime;
    }

    protected void appendExtendedTypesModule(
            Collection<Module> modules,
            Set<ExtendedType> types) {

        if(!types.isEmpty()) {
            modules.add(b -> {
                ListBuilder<ExtendedType> listBinder = ServerModule.contributeUserTypes(b);
                types.forEach(listBinder::add);
            });
        }
    }

    protected void appendValueObjectTypesModule(
            Collection<Module> modules,
            Set<ValueObjectType> types) {

        if(!types.isEmpty()) {
            modules.add(b -> {
                ListBuilder<ValueObjectType> listBinder = ServerModule.contributeValueObjectTypes(b);
                types.forEach(listBinder::add);
            });
        }
    }

    protected void appendQueryFiltersModule(
            Collection<Module> modules,
            Set<DataChannelQueryFilter> queryFilters) {

        modules.add(b -> {
            ListBuilder<DataChannelQueryFilter> listBinder = ServerModule.contributeDomainQueryFilters(b);
            queryFilters.forEach(listBinder::add);
        });
    }

    protected void appendSyncFiltersModule(
            Collection<Module> modules,
            Injector injector,
            Set<MappedDataChannelSyncFilter> syncFilters,
            Set<MappedDataChannelSyncFilterType> syncFilterTypes) {

        if (syncFilters.isEmpty() && syncFilterTypes.isEmpty()) {
            return;
        }

        List<MappedDataChannelSyncFilter> combined = new ArrayList<>(syncFilters.size() + syncFilterTypes.size());
        combined.addAll(syncFilters);
        syncFilterTypes.stream()
                .map(t -> new MappedDataChannelSyncFilter(injector.getInstance(t.getFilterType()), t.isIncludeInTransaction()))
                .forEach(combined::add);

        modules.add(b -> {
            ListBuilder<DataChannelSyncFilter> listBinder = ServerModule.contributeDomainSyncFilters(b);
            combined.forEach(mf -> {
                if (mf.isIncludeInTransaction()) {
                    listBinder.insertBefore(mf.getFilter(), TransactionFilter.class);
                } else {
                    listBinder.addAfter(mf.getFilter(), TransactionFilter.class);
                }
            });
        });
    }

    protected void appendCommitLogModules(
            Collection<Module> modules,
            Injector injector,
            Set<MappedCommitLogListener> commitLogListeners,
            Set<MappedCommitLogListenerType> commitLogListenerTypes) {

        if (commitLogListeners.isEmpty() && commitLogListenerTypes.isEmpty()) {
            return;
        }

        CommitLogModuleBuilder builder = new CommitLogModuleBuilder();
        commitLogListeners.forEach(builder::add);
        commitLogListenerTypes.forEach(t -> builder.add(t.resolve(injector)));

        builder.appendModules(modules);
    }
}
