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

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.jdbc.DataSourceFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@BQConfig("Configures Cayenne stack, providing injectable ServerRuntime.")
public class ServerRuntimeFactory {

    private static final String DEFAULT_CONFIG = "cayenne-project.xml";

    private String name;
    private Collection<String> configs;
    private List<DataMapConfig> maps;
    private String datasource;
    private boolean createSchema;

    public ServerRuntimeFactory() {
        this.configs = new ArrayList<>();
        this.maps = new ArrayList<>();
    }

    public ServerRuntime createCayenneRuntime(
            DataSourceFactory dataSourceFactory,
            CayenneConfigMerger configMerger,
            Collection<Module> extraModules,
            Collection<String> extraConfigs) {

        Collection<String> factoryConfigs = configs();

        return cayenneBuilder(dataSourceFactory)
                .addConfigs(configMerger.merge(factoryConfigs, extraConfigs))
                .addModules(extraModules)
                .build();
    }

    /**
     * Creates and returns a preconfigured {@link ServerRuntimeBuilder} with
     * Cayenne config, name, Java8 integration module and a DataSource. Override
     * to add custom modules, extra projects, etc.
     *
     * @param dataSourceFactory injected Bootique {@link DataSourceFactory}
     * @return a {@link ServerRuntimeBuilder} that can be extended in
     * subclasses.
     */
    protected ServerRuntimeBuilder cayenneBuilder(DataSourceFactory dataSourceFactory) {

        // building our own Cayenne extensions module...
        return ServerRuntime.builder(name).addModule(binder -> {

            // provide schema creation hook
            if (createSchema) {
                binder.bind(SchemaUpdateStrategyFactory.class).toInstance(descriptor -> new CreateIfNoSchemaStrategy());
            }

            DefaultDataSourceName defaultDataSourceName = defaultDataSourceName(dataSourceFactory);
            binder.bind(Key.get(DefaultDataSourceName.class)).toInstance(defaultDataSourceName);
            binder.bindList(DataMapConfig.class).addAll(maps);

            // provide default DataNode
            // TODO: copied from Cayenne, as the corresponding provider is not public or rather
            // until https://issues.apache.org/jira/browse/CAY-2095 is implemented
            binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);

            // Bootique DataSource hooks...
            BQCayenneDataSourceFactory bqCayenneDSFactory = new BQCayenneDataSourceFactory(dataSourceFactory, datasource);
            binder.bind(org.apache.cayenne.configuration.server.DataSourceFactory.class).toInstance(bqCayenneDSFactory);
        });
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

    /**
     * Sets an optional collection of Cayenne projects to load in runtime. If missing, will try to locate a file
     * 'cayenne-project.xml' on classpath.
     *
     * @param configs a collection of Cayenne config XML files.
     * @since 0.14
     */
    @BQConfigProperty("An optional collection of Cayenne projects to load in runtime. If missing, will try to locate a " +
            "file 'cayenne-project.xml' on classpath.")
    public void setConfigs(Collection<String> configs) {
        this.configs = configs;
    }

    /**
     * Sets a list of DataMaps that are included in the app runtime without an explicit refrence in  'cayenne-project.xml'.
     *
     * @param maps list of DataMap configs
     * @since 0.18
     */
    @BQConfigProperty("A list of DataMaps that are included in the app runtime without an explicit refrence in " +
            "'cayenne-project.xml'.")
    public void setMaps(List<DataMapConfig> maps) {
        this.maps = maps;
    }

    /**
     * Sets an optional name of the Cayenne stack we are created. This will be the name assigned to Cayenne DataDomain and
     * used in event dispatches, etc.
     *
     * @param name a name of Cayenne stack created by the factory.
     * @since 0.9
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
     * @since 0.11
     */
    @BQConfigProperty("Whether to attempt creation of the DB schema on startup based on Cayenne mapping. The default is " +
            "'false'. Automatic schema creation is often used in unit tests.")
    public void setCreateSchema(boolean createSchema) {
        this.createSchema = createSchema;
    }
}
