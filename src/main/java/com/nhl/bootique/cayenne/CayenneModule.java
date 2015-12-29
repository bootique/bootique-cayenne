package com.nhl.bootique.cayenne;

import java.util.Objects;

import org.apache.cayenne.configuration.server.ServerRuntime;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jdbc.DataSourceFactory;
import com.nhl.bootique.log.BootLogger;
import com.nhl.bootique.shutdown.ShutdownManager;

public class CayenneModule extends ConfigModule {

	private String config = "cayenne-project.xml";

	public CayenneModule() {
	}

	public CayenneModule(String configPrefix) {
		super(configPrefix);
	}

	/**
	 * @deprecated since 0.9 use {@link #configName(String)}.
	 * @param projectName
	 *            a name of Cayenne XML config file to use if it is not
	 *            initialized from YAML.
	 */
	@Deprecated
	public CayenneModule projectName(String projectName) {
		return configName(projectName);
	}

	/**
	 * @param config
	 *            a name of Cayenne XML config file to use if it is not
	 *            initialized from YAML.
	 * @since 0.9
	 * @return this instance
	 */
	public CayenneModule configName(String config) {
		Objects.requireNonNull(config);
		this.config = config;
		return this;
	}

	/**
	 * Ensures that Cayenne runtime is created without any ORM configuration. It
	 * is useful occasionally to have such a bare stack.
	 * 
	 * @since 0.9
	 * @return this instance.
	 */
	public CayenneModule noConfig() {
		this.config = null;
		return this;
	}

	@Provides
	@Singleton
	public ServerRuntime createCayenneRuntime(ConfigurationFactory configFactory, DataSourceFactory dataSourceFactory,
			BootLogger bootLogger, ShutdownManager shutdownManager) {
		
		ServerRuntime runtime = configFactory.config(ServerRuntimeFactory.class, configPrefix)
				.initConfigIfNotSet(config).createCayenneRuntime(dataSourceFactory);

		shutdownManager.addShutdownHook(() -> {
			bootLogger.trace(() -> "shutting down Cayenne...");
			runtime.shutdown();
		});

		return runtime;
	}

}
