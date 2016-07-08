package com.nhl.bootique.cayenne;

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
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.java8.CayenneJava8Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class CayenneModule extends ConfigModule {


    public CayenneModule() {
    }

    public CayenneModule(String configPrefix) {
        super(configPrefix);
    }

    /**
     * Returns a Guice {@link Multibinder} to add Cayenne DataChannelFilters.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for Cayenne DataChannelFilters
     * @since 0.13
     */
    public static Multibinder<DataChannelFilter> contributeFilters(Binder binder) {
        return Multibinder.newSetBinder(binder, DataChannelFilter.class);
    }

    /**
     * Returns a Guice {@link Multibinder} to add Cayenne annotated listeners.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for Cayenne annotated listeners.
     * @since 0.13
     */
    public static Multibinder<Object> contributeListeners(Binder binder) {
        return Multibinder.newSetBinder(binder, Object.class, CayenneListener.class);
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
                .createCayenneRuntime(dataSourceFactory, extras);

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
}
