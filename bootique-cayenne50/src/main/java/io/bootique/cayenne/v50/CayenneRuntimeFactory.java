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
import io.bootique.cayenne.v50.annotation.CayenneListener;
import io.bootique.cayenne.v50.commitlog.CommitLogModuleBuilder;
import io.bootique.cayenne.v50.commitlog.MappedCommitLogListener;
import io.bootique.cayenne.v50.commitlog.MappedCommitLogListenerType;
import io.bootique.cayenne.v50.syncfilter.MappedDataChannelSyncFilter;
import io.bootique.cayenne.v50.syncfilter.MappedDataChannelSyncFilterType;
import io.bootique.di.Injector;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.shutdown.ShutdownManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.CoreModuleExtender;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@BQConfig("Configures Cayenne stack, providing injectable CayenneRuntime.")
public class CayenneRuntimeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CayenneRuntimeFactory.class);

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
    public CayenneRuntimeFactory(
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
            A map of Cayenne DataMaps to Bootique DataSource names. It allows to link DataMaps to DataSources,
            creating a Cayenne DataNode per DataSource.""")
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

    @BQConfigProperty("An optional name of the DataSource to use in Cayenne for all the DataMaps not linked to a " +
            "DataSource via 'mapDatasources'. A DataSource with the matching name must be defined in 'bootique-jdbc' " +
            "configuration. If missing, and 'bootique-jdbc' defines exactly one DataSource, that DataSource is used.")
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

    public CayenneRuntime create() {

        CayenneRuntimeBuilder builder = CayenneRuntime.of(name);

        addBootiqueExtensions(builder);
        addDataNodes(builder);
        builder.addModules(customModules);
        addExtendedTypes(builder);
        addValueObjectTypes(builder);
        addQueryFilters(builder);
        addSyncFilters(builder);
        addCommitLog(builder);
        addListeners(builder);

        addLocations(builder);
        CayenneRuntime runtime = builder.build();

        shutdownManager.onShutdown(runtime, CayenneRuntime::shutdown);
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

    void addBootiqueExtensions(CayenneRuntimeBuilder builder) {
        builder.addModule(b -> b.bind(ResourceLocator.class).to(BQResourceLocator.class));
    }

    void addDataNodes(CayenneRuntimeBuilder builder) {

        String defaultDataSource = defaultDataSourceName();

        // Preserving the insertion order for predictable node creation
        Map<String, Set<String>> mapsByDataSource = new LinkedHashMap<>();
        if (mapDatasources != null) {
            mapDatasources.forEach((map, ds) -> mapsByDataSource
                    .computeIfAbsent(ds, n -> new LinkedHashSet<>())
                    .add(map));
        }

        // Nothing is explicitly mapped, and we can't guess the DataSource to use for the DataMaps. Complain right
        // away instead of leaving Cayenne without a default DataNode
        if (defaultDataSource == null && mapsByDataSource.isEmpty()) {
            throwOnAmbiguousDefaultDataSource();
        }

        mapsByDataSource.forEach((ds, maps) -> {

            // The default node picks up every DataMap not linked to another node, so DataMaps explicitly mapped to
            // the default DataSource need no separate node (and creating one would clash with the default node name)
            if (!ds.equals(defaultDataSource)) {
                builder.addDataNode(nodeDescriptor(ds), maps.toArray(new String[0]));
            }
        });

        if (defaultDataSource != null) {
            builder.defaultDataNode(nodeDescriptor(defaultDataSource));
        }
    }

    DataNodeDescriptor nodeDescriptor(String dataSourceName) {

        // Using the Bootique DataSource name for the node name. Distinguishing nodes by name may be useful in case of
        // multiple stacks used in the same transaction...

        DataNodeDescriptor.Builder builder = DataNodeDescriptor
                .of(dataSourceName)
                .dataSource(dataSource(dataSourceName));

        if (createSchema) {
            builder.createSchemaIfNeeded();
        }

        return builder.build();
    }

    void throwOnAmbiguousDefaultDataSource() {

        Collection<String> names = dataSourceFactory.allNames();

        // no DataSources at all is not an error. It is a valid case of a Cayenne stack without a DB connection
        if (!names.isEmpty()) {
            throw new IllegalStateException(
                    String.format("Can't map Cayenne DataSource: 'cayenne.datasource' is missing. " +
                            "Available DataSources are %s", names));
        }
    }

    DataSource dataSource(String dataSourceName) {
        DataSource ds = dataSourceFactory.forName(dataSourceName);
        if (ds == null) {
            throw new IllegalStateException("Unknown Cayenne DataSource: " + dataSourceName);
        }

        return ds;
    }

    void addExtendedTypes(CayenneRuntimeBuilder builder) {
        if (!extendedTypes.isEmpty()) {
            builder.addModule(b -> {
                CoreModuleExtender e = CoreModule.extend(b);
                extendedTypes.forEach(e::addUserExtendedType);
            });
        }
    }

    void addValueObjectTypes(CayenneRuntimeBuilder builder) {
        if (!valueObjectTypes.isEmpty()) {
            builder.addModule(b -> {
                CoreModuleExtender e = CoreModule.extend(b);
                valueObjectTypes.forEach(e::addValueObjectType);
            });
        }
    }

    void addQueryFilters(CayenneRuntimeBuilder builder) {
        builder.addModule(b -> {
            CoreModuleExtender e = CoreModule.extend(b);
            queryFilters.forEach(e::addQueryFilter);
        });
    }

    void addSyncFilters(CayenneRuntimeBuilder builder) {

        if (syncFilters.isEmpty() && syncFilterTypes.isEmpty()) {
            return;
        }

        List<MappedDataChannelSyncFilter> combined = new ArrayList<>(syncFilters.size() + syncFilterTypes.size());
        combined.addAll(syncFilters);
        syncFilterTypes.stream()
                .map(t -> new MappedDataChannelSyncFilter(injector.getInstance(t.getFilterType()), t.isIncludeInTransaction()))
                .forEach(combined::add);

        builder.addModule(b -> {
            CoreModuleExtender e = CoreModule.extend(b);
            combined.forEach(mf -> e.addSyncFilter(mf.getFilter(), mf.isIncludeInTransaction()));
        });
    }

    void addCommitLog(CayenneRuntimeBuilder builder) {

        if (commitLogListeners.isEmpty() && commitLogListenerTypes.isEmpty()) {
            return;
        }

        CommitLogModuleBuilder clmBuilder = new CommitLogModuleBuilder();
        commitLogListeners.forEach(clmBuilder::add);
        commitLogListenerTypes.forEach(t -> clmBuilder.add(t.resolve(injector)));

        if (injector.hasProvider(io.bootique.di.Key.get(Boolean.class, CayenneModuleExtender.COMMIT_LOG_ANNOTATION))) {
            clmBuilder.applyCommitLogAnnotation();
        }

        clmBuilder.addModules(builder);
    }

    void addListeners(CayenneRuntimeBuilder builder) {
        builder.addModule(b -> {
            CoreModuleExtender e = CoreModule.extend(b);
            listeners.forEach(e::addListener);
        });
    }

    void addLocations(CayenneRuntimeBuilder builder) {
        
        // there's no preference of injectedLocations vs config locations. They are simply combined together.
        // CayenneRuntimeBuilder ensures location uniqueness.

        builder.addConfigs(injectedLocations);

        if (locations != null) {
            builder.addConfigs(locations);
        }
    }
}
