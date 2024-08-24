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

package io.bootique.cayenne.v50;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.cayenne.v50.annotation.CayenneConfigs;
import io.bootique.cayenne.v50.annotation.CayenneListener;
import io.bootique.cayenne.v50.commitlog.CommitLogModuleBuilder;
import io.bootique.cayenne.v50.commitlog.MappedCommitLogListener;
import io.bootique.cayenne.v50.commitlog.MappedCommitLogListenerType;
import io.bootique.cayenne.v50.syncfilter.MappedDataChannelSyncFilter;
import io.bootique.cayenne.v50.syncfilter.MappedDataChannelSyncFilterType;
import io.bootique.di.Injector;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.shutdown.ShutdownManager;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.apache.cayenne.tx.TransactionFilter;

import javax.inject.Inject;
import java.util.*;

@BQConfig("Configures Cayenne stack, providing injectable CayenneRuntime.")
public class CayenneRuntimeFactory {

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
    public CayenneRuntimeFactory(
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
    @BQConfigProperty("An optional name of the Cayenne stack we are created. This will be the name assigned to Cayenne" +
            " DataDomain and used in event dispatches, etc.")
    public void setName(String name) {
        this.name = name;
    }

    @BQConfigProperty("An optional name of the DataSource to use in Cayenne. A DataSource with the matching name " +
            "must be defined in 'bootique-jdbc' configuration. If missing, a DataSource from Cayenne project or a " +
            "default DataSource from 'bootique-jdbc' is used.")
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * Sets a flag that defines whether to attempt creation of the DB schema on startup based on Cayenne mapping. The
     * default is 'false'. Automatic schema creation is often used in unit tests.
     *
     * @param createSchema if true, Cayenne will attempt to create database schema if it is missing.
     */
    @BQConfigProperty("Whether to attempt creation of the DB schema on startup based on Cayenne mapping. The default is " +
            "'false'. Automatic schema creation is often used in unit tests.")
    public void setCreateSchema(boolean createSchema) {
        this.createSchema = createSchema;
    }

    public CayenneRuntime create() {

        Collection<String> factoryConfigs = configs();

        Collection<Module> extras = new ArrayList<>(customModules);

        appendExtendedTypesModule(extras, extendedTypes);
        appendValueObjectTypesModule(extras, valueObjectTypes);

        appendQueryFiltersModule(extras, queryFilters);
        appendSyncFiltersModule(extras, injector, syncFilters, syncFilterTypes);
        appendCommitLogModules(extras, injector, commitLogListeners, commitLogListenerTypes);

        CayenneRuntime runtime = cayenneBuilder(dataSourceFactory)
                .addConfigs(configMerger.merge(factoryConfigs, injectedCayenneConfigs))
                .addModules(extras)
                .build();

        shutdownManager.onShutdown(runtime, CayenneRuntime::shutdown);

        // TODO: listeners should be wrapped in a CayenneModule and added to Cayenne via DI, just like filters...
        if (!listeners.isEmpty()) {
            DataDomain domain = runtime.getDataDomain();
            listeners.forEach(domain::addListener);
        }

        startupCallbacks.forEach(c -> c.onRuntimeCreated(runtime));

        return runtime;
    }

    /**
     * Creates and returns a preconfigured {@link CayenneRuntimeBuilder} with Cayenne config, name, Java8 integration
     * module and a DataSource. Override to add custom modules, extra projects, etc.
     *
     * @param dataSourceFactory injected Bootique {@link DataSourceFactory}
     * @return a {@link CayenneRuntimeBuilder} that can be extended in subclasses.
     */
    protected CayenneRuntimeBuilder cayenneBuilder(DataSourceFactory dataSourceFactory) {
        return CayenneRuntime.builder(name).addModule(factoryModule(dataSourceFactory));
    }

    protected Module factoryModule(DataSourceFactory dataSourceFactory) {
        return binder -> {
            // provide schema creation hook
            if (createSchema) {
                binder.bind(SchemaUpdateStrategyFactory.class).toInstance(descriptor -> new CreateIfNoSchemaStrategy());
            }

            DefaultDataSourceName defaultDataSourceName = defaultDataSourceName(dataSourceFactory);
            binder.bind(Key.get(DefaultDataSourceName.class)).toInstance(defaultDataSourceName);
            binder.bindMap(DataMapConfig.class).putAll(maps != null ? maps : Map.of());

            // provide default DataNode
            // TODO: copied from Cayenne, as the corresponding provider is not public or rather
            // until https://issues.apache.org/jira/browse/CAY-2095 is implemented
            binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);

            // Bootique DataSource hooks...
            BQCayenneDataSourceFactory bqCayenneDSFactory = new BQCayenneDataSourceFactory(dataSourceFactory, datasource);
            binder.bind(org.apache.cayenne.configuration.runtime.DataSourceFactory.class).toInstance(bqCayenneDSFactory);
        };
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

    DefaultDataSourceName defaultDataSourceName(DataSourceFactory dataSourceFactory) {

        if (datasource != null) {
            return new DefaultDataSourceName(datasource);
        }

        Collection<String> allNames = dataSourceFactory.allNames();
        if (allNames.size() == 1) {
            return new DefaultDataSourceName(allNames.iterator().next());
        }

        return new DefaultDataSourceName(null);
    }

    protected void appendExtendedTypesModule(Collection<Module> modules, Set<ExtendedType> types) {
        if (!types.isEmpty()) {
            modules.add(b -> {
                ListBuilder<ExtendedType> listBinder = CoreModule.contributeUserTypes(b);
                types.forEach(listBinder::add);
            });
        }
    }

    protected void appendValueObjectTypesModule(Collection<Module> modules, Set<ValueObjectType> types) {
        if (!types.isEmpty()) {
            modules.add(b -> {
                ListBuilder<ValueObjectType> listBinder = CoreModule.contributeValueObjectTypes(b);
                types.forEach(listBinder::add);
            });
        }
    }

    protected void appendQueryFiltersModule(Collection<Module> modules, Set<DataChannelQueryFilter> queryFilters) {
        modules.add(b -> {
            ListBuilder<DataChannelQueryFilter> listBinder = CoreModule.contributeDomainQueryFilters(b);
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
            ListBuilder<DataChannelSyncFilter> listBinder = CoreModule.contributeDomainSyncFilters(b);
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

        boolean applyCommitLogAnnotation = injector.hasProvider(
                io.bootique.di.Key.get(Boolean.class, CayenneModuleExtender.COMMIT_LOG_ANNOTATION));
        if (applyCommitLogAnnotation) {
            builder.applyCommitLogAnnotation();
        }

        builder.appendModules(modules);
    }
}
