package com.nhl.bootique.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.java8.CayenneJava8Module;

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

	/**
	 * @since 0.12
	 * @return a Builder instance to configure the module before using it to
	 *         initialize DI container.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @param configPrefix
	 *            YAML config prefix for CayenneModule.
	 * @since 0.12
	 * @return a Builder instance to configure the module before using it to
	 *         initialize DI container.
	 */
	public static Builder builder(String configPrefix) {
		return new Builder(configPrefix);
	}

	CayenneModule() {
	}

	public CayenneModule(String configPrefix) {
		super(configPrefix);
	}

	@Override
	public void configure(Binder binder) {
		// trigger extension points creation
		CayenneModule.contributeListeners(binder);
		CayenneModule.contributeFilters(binder);
	}

	@Provides
	@Singleton
	protected ServerRuntime createCayenneRuntime(ConfigurationFactory configFactory,
			DataSourceFactory dataSourceFactory, BootLogger bootLogger, ShutdownManager shutdownManager,
			@CayenneListener Set<Object> listeners, Set<DataChannelFilter> filters) {

		Collection<Module> extras = extraCayenneModules(filters);
		ServerRuntime runtime = configFactory.config(ServerRuntimeFactory.class, configPrefix)
				.initConfigIfNotSet(config).createCayenneRuntime(dataSourceFactory, extras);

		shutdownManager.addShutdownHook(() -> {
			bootLogger.trace(() -> "shutting down Cayenne...");
			runtime.shutdown();
		});

		// TODO: listeners should be really contributable to Cayenne via DI,
		// just like filters...
		if (!listeners.isEmpty()) {
			DataDomain domain = runtime.getDataDomain();
			listeners.forEach(l -> domain.addListener(l));
		}

		return runtime;
	}

	protected Collection<Module> extraCayenneModules(Set<DataChannelFilter> filters) {
		Collection<Module> extras = new ArrayList<>();
		extras.add(new CayenneJava8Module());

		if (!filters.isEmpty()) {
			extras.add(cayenneBinder -> {
				ListBuilder<DataChannelFilter> listBinder = cayenneBinder
						.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST);
				filters.forEach(f -> listBinder.add(f));
			});
		}

		return extras;
	}

	public static class Builder {
		private CayenneModule module;

		private Builder() {
			this.module = new CayenneModule();
		}

		private Builder(String configPrefix) {
			this.module = new CayenneModule(configPrefix);
		}

		public CayenneModule build() {
			return module;
		}

		/**
		 * Sets the name of Cayenne XML config file to use. Note that config
		 * name coming from YAML takes precedence over the setting passed via
		 * this method.
		 * 
		 * @param config
		 *            a name of Cayenne XML config file to use if it is not
		 *            initialized from YAML.
		 * @return this builder instance
		 */
		public Builder configName(String config) {
			module.config = config;
			return this;
		}

		/**
		 * Ensures that Cayenne runtime is created without any ORM
		 * configuration. It is useful occasionally to have such a bare stack.
		 * 
		 * @since 0.9
		 * @return this instance.
		 */
		public Builder noConfig() {
			module.config = null;
			return this;
		}
	}

}
