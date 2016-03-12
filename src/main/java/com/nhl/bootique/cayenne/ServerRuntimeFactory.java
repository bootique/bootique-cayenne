package com.nhl.bootique.cayenne;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.sql.DataSource;

import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhl.bootique.jdbc.DataSourceFactory;

public class ServerRuntimeFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerRuntimeFactory.class);

	private String name;
	private String config;
	private Collection<String> configs;
	private String datasource;
	private boolean createSchema;

	public ServerRuntime createCayenneRuntime(DataSourceFactory dataSourceFactory, Collection<Module> extraModules) {
		DataSource ds = locateDataSource(dataSourceFactory);
		return cayenneBuilder(ds).addModules(extraModules).build();
	}

	protected DataSource locateDataSource(DataSourceFactory dataSourceFactory) {

		// if no property is set, presumably Cayenne configures its own
		// DataSource without Bootique involvement
		if (datasource == null) {
			return null;
		}

		DataSource ds = dataSourceFactory.forName(datasource);
		if (ds == null) {
			throw new IllegalStateException("Unknown 'datasource': " + datasource);
		}

		return ds;
	}

	/**
	 * Creates and returns a preconfigured {@link ServerRuntimeBuilder} with
	 * Cayenne config, name, Java8 integration module and a DataSource. Override
	 * to add custom modules, extra projects, etc.
	 * 
	 * @param dataSource
	 *            A {@link DataSource}, which is usually provided by Spring.
	 * @return a {@link ServerRuntimeBuilder} that can be extended in
	 *         subclasses.
	 */
	protected ServerRuntimeBuilder cayenneBuilder(DataSource dataSource) {
		ServerRuntimeBuilder builder = ServerRuntimeBuilder.builder(name);

		configs().forEach(config -> builder.addConfig(config));

		if (createSchema) {
			builder.addModule(binder -> binder.bind(SchemaUpdateStrategy.class).to(CreateIfNoSchemaStrategy.class));
		}

		if (dataSource != null) {
			builder.dataSource(dataSource);
		}

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

		return configs;
	}

	/**
	 * Conditionally initializes Cayenne config name if it is null.
	 * 
	 * @param config
	 *            a name of Cayenne XML config file to use if config was not
	 *            already initialized.
	 * 
	 * @since 0.9
	 */
	public ServerRuntimeFactory initConfigIfNotSet(String config) {

		if (config != null) {
			if (this.config == null && (configs == null || configs.isEmpty())) {
				this.configs = Collections.singletonList(config);
			}
		}

		return this;
	}

	/**
	 * @since 0.9
	 * @param config
	 *            a name of the Cayenne config XML file.
	 * 
	 * @deprecated since 0.14 in favor of {@link #setConfigs(Collection)}.
	 */
	public void setConfig(String config) {
		this.config = config;
	}

	/**
	 * @since 0.14
	 * @param configs
	 *            a collection of Cayenne config XML files.
	 */
	public void setConfigs(Collection<String> configs) {
		this.configs = configs;
	}

	/**
	 * Sets an optional name of the Cayenne stack to be created. It is
	 * occasionally useful to name Cayenne stacks.
	 * 
	 * @param name
	 *            a name of Cayenne stack created by the factory.
	 * @since 0.9
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	/**
	 * @param createSchema
	 *            if true, Cayenne will attempt to create database schema if it
	 *            is missing.
	 * @since 0.11
	 */
	public void setCreateSchema(boolean createSchema) {
		this.createSchema = createSchema;
	}
}
