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
import jakarta.inject.Named;
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
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.tx.TransactionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@BQConfig("Configures Cayenne stack, providing injectable ServerRuntime.")
public class ServerRuntimeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRuntimeFactory.class);

    private final Injector injector;
    private final ShutdownManager shutdownManager;
    private final DataSourceFactory dataSourceFactory;
    private final Set<String> injectedLocations;
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
    private String datasource;
    private boolean createSchema;
    private List<String> locations;
    private Map<String, String> mapDatasources;


    @Inject
    public ServerRuntimeFactory(
            Injector injector,
            ShutdownManager shutdownManager,
            DataSourceFactory dataSourceFactory,
            @Named(CayenneModule.LOCATIONS_BINDING) Set<String> injectedLocations,
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
        this.injectedLocations = injectedLocations;
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
     * @since 4.0
     */
    @BQConfigProperty("A list of Cayenne project file locations specified in Bootique resource format")
    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    /**
     * @since 4.0
     */
    @BQConfigProperty("""
            A map of Cayenne DataMaps to Bootique DataSource names. It allows to link DataMaps to DataSources as
            well as override any DataNode mappings in Cayenne project files.""")
    public void setMapDatasources(Map<String, String> mapDatasources) {
        this.mapDatasources = mapDatasources;
    }

    @BQConfigProperty("** Deprecated and ignored. Use 'cayenne.locations' instead")
    @Deprecated(since = "4.0", forRemoval = true)
    public void setConfigs(Collection<String> configs) {
        LOGGER.warn("""
                ** 'cayenne.configs' configuration property is deprecated in favor of 'cayenne.locations' 
                that are specified as Bootique resources""");

        String cpPrefix = "classpath:";
        List<String> locations = configs != null
                ? configs.stream().map(c -> c.startsWith(cpPrefix) ? c : cpPrefix + c).toList()
                : null;

        setLocations(locations);
    }


    @BQConfigProperty("** Deprecated and ignored. Partially replaced by 'cayenne.mapDatasources'")
    @Deprecated(since = "4.0", forRemoval = true)
    public void setMaps(Map<String, DataMapConfig> maps) {
        LOGGER.warn("""
                ** 'cayenne.maps' configuration property is deprecated and ignored. It is partially replaced by
                'cayenne.mapDatasources'""");
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

        addLocations(builder);

        ServerRuntime runtime = builder.build();

        shutdownManager.onShutdown(runtime, ServerRuntime::shutdown);
        startupCallbacks.forEach(c -> c.onRuntimeCreated(runtime));

        return runtime;
    }

    String defaultDataSourceName() {

        if (datasource != null) {
            return datasource;
        }

        Collection<String> allNames = dataSourceFactory.allNames();
        if (allNames.size() == 1) {
            return allNames.iterator().next();
        }

        return null;
    }

    void addCreateSchema(ServerRuntimeBuilder builder) {
        if (createSchema) {
            builder.addModule(b -> b.bind(SchemaUpdateStrategyFactory.class).toInstance(descriptor -> new CreateIfNoSchemaStrategy()));
        }
    }

    void addBootiqueExtensions(ServerRuntimeBuilder builder) {
        String defaultDS = defaultDataSourceName();
        var ddProvider = new SyntheticNodeDataDomainProvider(defaultDS, mapDatasources != null ? mapDatasources : Map.of());
        var dsFactory = new BQCayenneDataSourceFactory(dataSourceFactory, datasource);

        builder.addModule(b -> {
            b.bind(ResourceLocator.class).to(BQResourceLocator.class);
            b.bind(DataDomain.class).toProviderInstance(ddProvider);
            b.bind(org.apache.cayenne.configuration.server.DataSourceFactory.class).toInstance(dsFactory);
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

    void addLocations(ServerRuntimeBuilder builder) {

        // there's no preference of injectedLocations vs config locations. They are simply combined together.
        // CayenneRuntimeBuilder ensures location uniqueness.

        builder.addConfigs(injectedLocations);
        if (locations != null) {
            builder.addConfigs(locations);
        }
    }
}
