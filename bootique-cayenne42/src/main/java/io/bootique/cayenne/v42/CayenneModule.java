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

import io.bootique.ConfigModule;
import io.bootique.cayenne.v42.annotation.CayenneConfigs;
import io.bootique.cayenne.v42.annotation.CayenneListener;
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
import org.apache.cayenne.commitlog.CommitLogFilter;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.meta.IncludeAllCommitLogEntityFactory;
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
public class CayenneModule extends ConfigModule {

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneModuleExtender} that can be used to load Cayenne custom extensions.
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
            Set<MappedCommitLogListenerType> commitLogListenerTypes) {

        Collection<Module> extras = new ArrayList<>(customModules);

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

        CommitLogModuleExtenders extenders = new CommitLogModuleExtenders(modules);

        for (MappedCommitLogListener ml : commitLogListeners) {
            extenders.get(ml.isIncludeInTransaction()).add(ml.getListener());
        }

        for (MappedCommitLogListenerType mlt : commitLogListenerTypes) {
            CommitLogListener listener = injector.getInstance(mlt.getListenerType());
            extenders.get(mlt.isIncludeInTransaction()).add(listener);
        }

        extenders.appendModules();
    }

    static class CommitLogModuleExtenders {

        private final Collection<Module> modules;
        private List<CommitLogListener> preTx;
        private List<CommitLogListener> postTx;

        CommitLogModuleExtenders(Collection<Module> modules) {
            this.modules = modules;
        }

        void appendModules() {
            if (preTx != null && !preTx.isEmpty()) {
                modules.add(preTxModule());
            }

            if (postTx != null && !postTx.isEmpty()) {
                modules.add(postTxModule());
            }
        }

        List<CommitLogListener> get(boolean includeInTx) {
            return includeInTx ? getPreTx() : getPostTx();
        }

        private List<CommitLogListener> getPreTx() {
            if (preTx == null) {
                preTx = new ArrayList<>();
            }

            return preTx;
        }

        private List<CommitLogListener> getPostTx() {
            if (postTx == null) {
                postTx = new ArrayList<>();
            }

            return postTx;
        }

        private Module preTxModule() {

            // TODO: reimplementing CommitLogModuleExtender.module() to allow both pre and post commit filters.
            //   Maybe this should go to Cayenne?

            return binder -> {
                CommitLogFilter filter = new CommitLogFilter(new IncludeAllCommitLogEntityFactory(), preTx);
                ServerModule.contributeDomainSyncFilters(binder).insertBefore(filter, TransactionFilter.class);
            };
        }

        private Module postTxModule() {

            // TODO: reimplementing CommitLogModuleExtender.module() to allow both pre and post commit filters.
            //   Maybe this should go to Cayenne?

            return binder -> {
                CommitLogFilter filter = new CommitLogFilter(new IncludeAllCommitLogEntityFactory(), postTx);
                ServerModule.contributeDomainSyncFilters(binder).addAfter(filter, TransactionFilter.class);
            };
        }
    }
}
