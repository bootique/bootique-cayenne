package com.nhl.launcher.cayenne;

import org.apache.cayenne.configuration.server.ServerRuntime;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.nhl.launcher.config.FactoryConfigurationService;

public class CayenneBundle {

	private static final String CONFIG_PREFIX = "cayenne";

	private String configPrefix;

	public static Module cayenneModule() {
		return cayenneModule(CONFIG_PREFIX);
	}

	public static Module cayenneModule(String configPrefix) {
		return new CayenneBundle(configPrefix).module();
	}

	private CayenneBundle(String configPrefix) {
		this.configPrefix = configPrefix;
	}

	public Module module() {
		return new CayenneModule();
	}

	class CayenneModule implements Module {

		@Override
		public void configure(Binder binder) {
			// do nothing.. configuration happens in @Provides
		}

		@Provides
		public ServerRuntime createCayenneRuntime(FactoryConfigurationService configService) {
			return configService.factory(CayenneFactory.class, configPrefix).createCayenneRuntime();
		}
	}
}
