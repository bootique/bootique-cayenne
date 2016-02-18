package com.nhl.bootique.cayenne;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.configuration.server.ServerRuntime;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.ConfigModule;
import com.nhl.bootique.cayenne.annotation.CayenneListener;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jdbc.DataSourceFactory;
import com.nhl.bootique.log.BootLogger;
import com.nhl.bootique.shutdown.ShutdownManager;

public class CayenneModule extends ConfigModule {

	private String config = "cayenne-project.xml";

	/**
	 * Returns a Guice {@link Multibinder} to add Cayenne DataChannelFilters.
	 * 
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.13
	 * @return returns a {@link Multibinder} for Cayenne DataChannelFilters
	 */
	public static Multibinder<DataChannelFilter> contributeFilters(Binder binder) {
		return Multibinder.newSetBinder(binder, DataChannelFilter.class);
	}

	/**
	 * Returns a Guice {@link Multibinder} to add Cayenne annotated listeners.
	 * 
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.13
	 * @return returns a {@link Multibinder} for Cayenne annotated listeners.
	 */
	public static Multibinder<Object> contributeListeners(Binder binder) {
		return Multibinder.newSetBinder(binder, Object.class, CayenneListener.class);
	}

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

	@Override
	public void configure(Binder binder) {
		// trigger extension points creation
		CayenneModule.contributeListeners(binder);
		CayenneModule.contributeFilters(binder);
	}

	@Provides
	@Singleton
	public ServerRuntime createCayenneRuntime(ConfigurationFactory configFactory, DataSourceFactory dataSourceFactory,
			BootLogger bootLogger, ShutdownManager shutdownManager, Set<Object> listeners) {

		ServerRuntime runtime = configFactory.config(ServerRuntimeFactory.class, configPrefix)
				.initConfigIfNotSet(config).createCayenneRuntime(dataSourceFactory, Collections.emptyList());

		shutdownManager.addShutdownHook(() -> {
			bootLogger.trace(() -> "shutting down Cayenne...");
			runtime.shutdown();
		});

		return runtime;
	}

}
