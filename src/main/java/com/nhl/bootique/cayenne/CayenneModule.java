package com.nhl.bootique.cayenne;

import org.apache.cayenne.configuration.server.ServerRuntime;

import com.google.inject.Provides;
import com.nhl.bootique.FactoryModule;
import com.nhl.bootique.factory.FactoryConfigurationService;

public class CayenneModule extends FactoryModule<ServerRuntimeFactory> {

	private String projectName;

	public CayenneModule() {
		super(ServerRuntimeFactory.class);
	}

	public CayenneModule(String configPrefix) {
		super(ServerRuntimeFactory.class, configPrefix);
	}

	public CayenneModule projectName(String projectName) {
		this.projectName = projectName;
		return this;
	}

	@Provides
	public ServerRuntime createCayenneRuntime(FactoryConfigurationService configService) {
		return createFactory(configService).initProjectIfNotSet(projectName).createCayenneRuntime();
	}

}
