package io.bootique.cayenne;

import io.bootique.jdbc.DataSourceFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class ServerRuntimeFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRuntimeFactory.class);
    private static final String DEFAULT_CONFIG = "cayenne-project.xml";

    private String name;

    @Deprecated
    private String config;

    private Collection<String> configs;
    private String datasource;
    private boolean createSchema;

    public ServerRuntime createCayenneRuntime(DataSourceFactory dataSourceFactory, Collection<Module> extraModules) {
        return cayenneBuilder(dataSourceFactory).addModules(extraModules).build();
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
        ServerRuntimeBuilder builder = ServerRuntimeBuilder.builder(name);

        configs().forEach(config -> builder.addConfig(config));

        // building our own Cayenne extensions module...
        builder.addModule(binder -> {

            // provide schema creation hook
            if (createSchema) {
                binder.bind(SchemaUpdateStrategy.class).to(CreateIfNoSchemaStrategy.class);
            }

            // provide default DataNode
            // TODO: copied from Cayenne, as the corresponding provider is not public or rather
            // until https://issues.apache.org/jira/browse/CAY-2095 is implemented
            binder.bind(DataDomain.class).toProvider(SyntheticNodeDataDomainProvider.class);

            // Bootique DataSource hooks...
            BQCayenneDataSourceFactory bqCayenneDSFactory =
                    new BQCayenneDataSourceFactory(dataSourceFactory, datasource);
            binder.bind(org.apache.cayenne.configuration.server.DataSourceFactory.class).toInstance(bqCayenneDSFactory);
        });

        return builder;
    }

    Collection<String> configs() {

        // order is important, so using ordered set...
        Collection<String> configs = new LinkedHashSet<>();

        if (this.configs != null) {
            configs.addAll(this.configs);
        }

        if (this.config != null) {
            LOGGER.warn("'config' key is deprecated. Use 'configs' instead");
            configs.add(config);
        }

        return configs.isEmpty() ? defaultConfigs() : configs;
    }

    Collection<String> defaultConfigs() {
        return getClass().getClassLoader().getResource(DEFAULT_CONFIG) != null
                ? Collections.singleton(DEFAULT_CONFIG)
                : Collections.emptySet();
    }

    /**
     * @param config a name of the Cayenne config XML file.
     * @since 0.9
     * @deprecated since 0.14 in favor of {@link #setConfigs(Collection)}.
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * @param configs a collection of Cayenne config XML files.
     * @since 0.14
     */
    public void setConfigs(Collection<String> configs) {
        this.configs = configs;
    }

    /**
     * Sets an optional name of the Cayenne stack to be created. It is
     * occasionally useful to name Cayenne stacks.
     *
     * @param name a name of Cayenne stack created by the factory.
     * @since 0.9
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * @param createSchema if true, Cayenne will attempt to create database schema if it
     *                     is missing.
     * @since 0.11
     */
    public void setCreateSchema(boolean createSchema) {
        this.createSchema = createSchema;
    }
}
