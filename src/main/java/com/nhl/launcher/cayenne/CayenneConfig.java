package com.nhl.launcher.cayenne;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.java8.CayenneJava8Module;

import com.nhl.launcher.cayenne.datasource.PoolingDataSourceConfig;

public class CayenneConfig {

	private String project;
	private PoolingDataSourceConfig datasource;

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
		String project = this.project != null ? this.project : "cayenne-project.xml";
		return ServerRuntimeBuilder.builder().addModule(new CayenneJava8Module()).addConfig(project)
				.dataSource(dataSource);
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

	public PoolingDataSourceConfig getDatasource() {
		return datasource;
	}

	public void setDatasource(PoolingDataSourceConfig datasource) {
		this.datasource = datasource;
	}
}
