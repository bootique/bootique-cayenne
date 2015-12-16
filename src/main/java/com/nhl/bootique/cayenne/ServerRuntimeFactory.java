package com.nhl.bootique.cayenne;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.java8.CayenneJava8Module;

import com.nhl.bootique.cayenne.datasource.PoolingDataSourceFactory;

public class ServerRuntimeFactory {

	static final String DEFAULT_PROJECT_NAME = "cayenne-project.xml";

	private String project;
	private PoolingDataSourceFactory datasource;

	/**
	 * Creates and returns a preconfigured {@link ServerRuntimeBuilder} with
	 * project, java8 integration and a DataSource. Override to add custom
	 * modules, extra projects, etc.
	 * 
	 * @param dataSource
	 *            A {@link DataSource}, which is usually provided by Spring.
	 * @return a {@link ServerRuntimeBuilder} that can be extended in
	 *         subclasses.
	 */
	protected ServerRuntimeBuilder cayenneBuilder(DataSource dataSource) {
		String project = this.project != null ? this.project : DEFAULT_PROJECT_NAME;
		return ServerRuntimeBuilder.builder().addModule(new CayenneJava8Module()).addConfig(project)
				.dataSource(dataSource);
	}

	public ServerRuntimeFactory initProjectIfNotSet(String project) {

		if (this.project == null) {
			this.project = project;
		}

		return this;
	}

	public ServerRuntime createCayenneRuntime() {
		DataSource ds = datasource.toDataSource();
		return cayenneBuilder(ds).build();
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public void setDatasource(PoolingDataSourceFactory datasource) {
		this.datasource = datasource;
	}
}
