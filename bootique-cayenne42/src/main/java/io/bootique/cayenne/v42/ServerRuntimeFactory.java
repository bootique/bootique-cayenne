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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.cayenne.v42.annotation.CayenneConfigs;
import io.bootique.cayenne.v42.annotation.CayenneListener;
import io.bootique.cayenne.v42.commitlog.CommitLogModuleBuilder;
import io.bootique.cayenne.v42.commitlog.MappedCommitLogListener;
import io.bootique.cayenne.v42.commitlog.MappedCommitLogListenerType;
import io.bootique.cayenne.v42.syncfilter.MappedDataChannelSyncFilter;
import io.bootique.cayenne.v42.syncfilter.MappedDataChannelSyncFilterType;
import io.bootique.di.Injector;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.shutdown.ShutdownManager;
import jakarta.inject.Inject;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@BQConfig("Configures Cayenne stack, providing injectable ServerRuntime.")
public class ServerRuntimeFactory {

    private static final String DEFAULT_CONFIG = "cayenne-project.xml";

    private final Injector injector;
    private final ShutdownManager shutdownManager;
    private final DataSourceFactory dataSourceFactory;
    private final CayenneConfigMerger configMerger;
    private final Set<String> injectedCayenneConfigs;
    private final Set<Module> customModules;
    private final Set<Object> listeners;
    private final Set<DataChannelQueryFilter> queryFilters;
    private final Set<MappedDataChannelSyncFilter> syncFilters;
    private final Set<MappedDataChannelSyncFilterType> syncFilterTypes;
    private final Set<CayenneStartupListener> startupCallbacks;
    private final Set<MappedCommitLogListener> commitLogListeners;
    private final Set<MappedCommitLogListenerType> commitLogListenerTypes;
    private final Set<ExtendedType> extendedTypes;
    private final Set<ValueObjectType> valueObjectTypes;

    private String name;
    private Collection<String> configs;
    private Map<String, DataMapConfig> maps;
    private String datasource;
    private boolean createSchema;

    @Inject
    public ServerRuntimeFactory(
            Injector injector,
            ShutdownManager shutdownManager,
            DataSourceFactory dataSourceFactory,
            CayenneConfigMerger configMerger,
            @CayenneConfigs Set<String> injectedCayenneConfigs,
            Set<Module> customModules,
            @CayenneListener Set<Object> listeners,
            Set<DataChannelQueryFilter> queryFilters,
            Set<MappedDataChannelSyncFilter> syncFilters,
            Set<MappedDataChannelSyncFilterType> syncFilterTypes,
            Set<CayenneStartupListener> startupCallbacks,
            Set<MappedCommitLogListener> commitLogListeners,
            Set<MappedCommitLogListenerType> commitLogListenerTypes,
            Set<ExtendedType> extendedTypes,
            Set<ValueObjectType> valueObjectTypes) {

        this.injector = injector;

        this.shutdownManager = shutdownManager;
        this.dataSourceFactory = dataSourceFactory;
        this.configMerger = configMerger;
        this.injectedCayenneConfigs = injectedCayenneConfigs;
        this.customModules = customModules;
        this.listeners = listeners;
        this.queryFilters = queryFilters;
        this.syncFilters = syncFilters;
        this.syncFilterTypes = syncFilterTypes;
        this.startupCallbacks = startupCallbacks;
        this.commitLogListeners = commitLogListeners;
        this.commitLogListenerTypes = commitLogListenerTypes;
        this.extendedTypes = extendedTypes;
        this.valueObjectTypes = valueObjectTypes;
    }

    /**
     * Sets an optional collection of Cayenne projects to load in runtime. If missing, will try to locate a file
     * 'cayenne-project.xml' on classpath.
     *
     * @param configs a collection of Cayenne config XML files.
     */
    @BQConfigProperty("An optional collection of Cayenne projects to load in runtime. If missing, will try to locate a " +
            "file 'cayenne-project.xml' on classpath.")
    public void setConfigs(Collection<String> configs) {
        this.configs = configs;
    }

    /**
     * Sets a map of DataMaps that are included in the app runtime without an explicit refrence in  'cayenne-project.xml'.
     *
     * @param maps map of DataMap configs
     */
    @BQConfigProperty("A list of DataMaps that are included in the app runtime without an explicit refrence in " +
            "'cayenne-project.xml'.")
    public void setMaps(Map<String, DataMapConfig> maps) {
        this.maps = maps;
    }

    /**
     * Sets an optional name of the Cayenne stack we are created. This will be the name assigned to Cayenne DataDomain and
     * used in event dispatches, etc.
     *
     * @param name a name of Cayenne stack created by the factory.
     */
    @BQConfigProperty("""
            An optional name of the Cayenne stack we are creating. This will be the name assigned to Cayenne DataDomain
            and used in event dispatches, etc.""")
    public void setName(String name) {
        this.name = name;
    }

    @BQConfigProperty("""
            An optional name of the DataSource to use in Cayenne. A DataSource with the matching name
            must be defined in 'bootique-jdbc' configuration. If missing, a DataSource from Cayenne project or a
            default DataSource from 'bootique-jdbc' is used.""")
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * Sets a flag that defines whether to attempt creation of the DB schema on startup based on Cayenne mapping. The
     * default is 'false'. Automatic schema creation is often used in unit tests.
     *
     * @param createSchema if true, Cayenne will attempt to create database schema if it is missing.
     */
    @BQConfigProperty("""
            Whether to attempt creation of the DB schema on startup based on Cayenne mapping. The default is
            'false'. Automatic schema creation is often used in unit tests.""")
    public void setCreateSchema(boolean createSchema) {
        this.createSchema = createSchema;
    }

    public ServerRuntime create() {

        Collection<String> factoryConfigs = configs();

        ServerRuntimeBuilder builder = ServerRuntime.builder(name);

        addCreateSchema(builder);
        addBootiqueExtensions(builder);
        builder.addModules(customModules);
        addExtendedTypes(builder);
        addValueObjectTypes(builder);
        addQueryFilters(builder);
        addSyncFilters(builder);
        addCommitLog(builder);
        addListeners(builder);

        ServerRuntime runtime = builder
                .addConfigs(configMerger.merge(factoryConfigs, injectedCayenneConfigs))
                .build();

        shutdownManager.onShutdown(runtime, ServerRuntime::shutdown);
        startupCallbacks.forEach(c -> c.onRuntimeCreated(runtime));

        return runtime;
    }

    Collection<String> configs() {

        // order is important, so using ordered set...
        Collection<String> configs = new LinkedHashSet<>();

        if (this.configs != null) {
            configs.addAll(this.configs);
        }

        return configs.isEmpty() ? defaultConfigs() : configs;
    }

    Collection<String> defaultConfigs() {

        // #54: if "maps" are specified explicitly, default config should be ignored

        if (maps != null && !maps.isEmpty()) {
            return Collections.emptySet();
        }

        return getClass().getClassLoader().getResource(DEFAULT_CONFIG) != null
                ? Collections.singleton(DEFAULT_CONFIG)
                : Collections.emptySet();
    }

    DefaultDataSourceName defaultDataSourceName() {

        if (datasource != null) {
            return new DefaultDataSourceName(datasource);
        }

        Collection<String> allNames = dataSourceFactory.allNames();
        if (allNames.size() == 1) {
            return new DefaultDataSourceName(allNames.iterator().next());
        }

        return new DefaultDataSourceName(null);
    }

    void addCreateSchema(ServerRuntimeBuilder builder) {
        if (createSchema) {
            builder.addModule(b -> b.bind(SchemaUpdateStrategyFactory.class).toInstance(descriptor -> new CreateIfNoSchemaStrategy()));
        }
    }

    void addBootiqueExtensions(ServerRuntimeBuilder builder) {
        DefaultDataSourceName defaultDataSourceName = defaultDataSourceName();

        builder.addModule(b -> {

            b.bind(Key.get(DefaultDataSourceName.class)).toInstance(defaultDataSourceName);
            b.bindMap(DataMapConfig.class).putAll(maps != null ? maps : Map.of());

            // provide default DataNode
            // TODO: copied from Cayenne, as the corresponding provider is not public or rather
            // until https://issues.apache.org/jira/browse/CAY-2095 is implemented
            b.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);

            // Bootique DataSource hooks...
            BQCayenneDataSourceFactory bqCayenneDSFactory = new BQCayenneDataSourceFactory(dataSourceFactory, datasource);
            b.bind(org.apache.cayenne.configuration.server.DataSourceFactory.class).toInstance(bqCayenneDSFactory);
        });
    }

    void addExtendedTypes(ServerRuntimeBuilder builder) {
        if (!extendedTypes.isEmpty()) {
            builder.addModule(b -> {
                ListBuilder<ExtendedType> listBinder = ServerModule.contributeUserTypes(b);
                extendedTypes.forEach(listBinder::add);
            });
        }
    }

    void addValueObjectTypes(ServerRuntimeBuilder builder) {
        if (!valueObjectTypes.isEmpty()) {
            builder.addModule(b -> {
                ListBuilder<ValueObjectType> listBinder = ServerModule.contributeValueObjectTypes(b);
                valueObjectTypes.forEach(listBinder::add);
            });
        }
    }

    void addQueryFilters(ServerRuntimeBuilder builder) {
        builder.addModule(b -> {
            ListBuilder<DataChannelQueryFilter> listBinder = ServerModule.contributeDomainQueryFilters(b);
            queryFilters.forEach(listBinder::add);
        });
    }

    void addSyncFilters(ServerRuntimeBuilder builder) {

        if (syncFilters.isEmpty() && syncFilterTypes.isEmpty()) {
            return;
        }

        List<MappedDataChannelSyncFilter> combined = new ArrayList<>(syncFilters.size() + syncFilterTypes.size());
        combined.addAll(syncFilters);
        syncFilterTypes.stream()
                .map(t -> new MappedDataChannelSyncFilter(injector.getInstance(t.getFilterType()), t.isIncludeInTransaction()))
                .forEach(combined::add);

        builder.addModule(b -> {
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

    void addCommitLog(ServerRuntimeBuilder builder) {

        if (commitLogListeners.isEmpty() && commitLogListenerTypes.isEmpty()) {
            return;
        }

        CommitLogModuleBuilder clmBuilder = new CommitLogModuleBuilder();
        commitLogListeners.forEach(clmBuilder::add);
        commitLogListenerTypes.forEach(t -> clmBuilder.add(t.resolve(injector)));

        boolean applyCommitLogAnnotation = injector.hasProvider(
                io.bootique.di.Key.get(Boolean.class, CayenneModuleExtender.COMMIT_LOG_ANNOTATION));
        if (applyCommitLogAnnotation) {
            clmBuilder.applyCommitLogAnnotation();
        }

        clmBuilder.addModules(builder);
    }

    void addListeners(ServerRuntimeBuilder builder) {
        builder.addModule(b -> {
            ListBuilder<Object> listBinder = ServerModule.contributeDomainListeners(b);
            listeners.forEach(listBinder::add);
        });
    }
}
