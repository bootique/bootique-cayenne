package com.nhl.bootique.cayenne;

import org.apache.cayenne.configuration.server.ServerRuntime;

import com.google.inject.Provides;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jdbc.DataSourceFactory;

public class CayenneModule extends ConfigModule {

	private String projectName;

	public CayenneModule() {
	}

	public CayenneModule(String configPrefix) {
		super(configPrefix);
	}

	public CayenneModule projectName(String projectName) {
		this.projectName = projectName;
		return this;
	}

	@Provides
	public ServerRuntime createCayenneRuntime(ConfigurationFactory configFactory, DataSourceFactory dataSourceFactory) {
		return configFactory.config(ServerRuntimeFactory.class, configPrefix).initProjectIfNotSet(projectName)
				.createCayenneRuntime(dataSourceFactory);
	}

}
