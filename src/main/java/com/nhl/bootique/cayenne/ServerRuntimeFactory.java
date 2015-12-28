package com.nhl.bootique.cayenne;

import java.util.Objects;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.java8.CayenneJava8Module;

import com.nhl.bootique.jdbc.DataSourceFactory;

public class ServerRuntimeFactory {

	private String name;
	private String config;
	private String datasource;

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
		ServerRuntimeBuilder builder = ServerRuntimeBuilder.builder(name).addModule(new CayenneJava8Module());

		// allow no-config stacks.. very useful sometimes
		if (config != null) {
			builder.addConfig(config);
		}

		return builder.dataSource(dataSource);
	}

	/**
	 * @deprecated since 0.9 use {@link #initConfigIfNotSet(String)}.
	 * @param project
	 *            a name of Cayenne XML config file to use if config was not
	 *            already initialized.
	 */
	@Deprecated
	public ServerRuntimeFactory initProjectIfNotSet(String project) {
		return initConfigIfNotSet(project);
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

		if (this.config == null) {
			this.config = config;
		}

		return this;
	}

	public ServerRuntime createCayenneRuntime(DataSourceFactory dataSourceFactory) {
		Objects.requireNonNull(datasource, "'datasource' property is null");
		DataSource ds = dataSourceFactory.forName(datasource);
		return cayenneBuilder(ds).build();
	}

	/**
	 * @deprecated since 0.9 use {@link #setConfig(String)}.
	 * @param project
	 *            a name of the Cayenne config XML file.
	 */
	@Deprecated
	public void setProject(String project) {
		setConfig(project);
	}

	/**
	 * @since 0.9
	 * @param config
	 *            a name of the Cayenne config XML file.
	 */
	public void setConfig(String config) {
		this.config = config;
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
}
