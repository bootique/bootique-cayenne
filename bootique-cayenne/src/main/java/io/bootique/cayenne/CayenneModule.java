package io.bootique.cayenne;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.ConfigModule;
import io.bootique.cayenne.annotation.CayenneConfigs;
import io.bootique.cayenne.annotation.CayenneListener;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.log.BootLogger;
import io.bootique.shutdown.ShutdownManager;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;

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
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneModuleExtender} that can be used to load Cayenne custom extensions.
     * @since 0.19
     */
    public static CayenneModuleExtender extend(Binder binder) {
        return new CayenneModuleExtender(binder);
    }

    @Override
    public void configure(Binder binder) {
        extend(binder).initAllExtensions();
    }

    @Provides
    @Singleton
    CayenneConfigMerger provideConfigMerger() {
        return new CayenneConfigMerger();
    }

    @Provides
    @Singleton
    protected ServerRuntime createCayenneRuntime(ConfigurationFactory configFactory,
                                                 DataSourceFactory dataSourceFactory,
                                                 BootLogger bootLogger,
                                                 ShutdownManager shutdownManager,
                                                 Set<Module> customModules,
                                                 @CayenneListener Set<Object> listeners,
                                                 Set<DataChannelFilter> filters,
                                                 CayenneConfigMerger configMerger,
                                                 @CayenneConfigs Set<String> injectedCayenneConfigs) {

        Collection<Module> extras = extraCayenneModules(customModules, filters);
        ServerRuntime runtime = configFactory
                .config(ServerRuntimeFactory.class, configPrefix)
                .createCayenneRuntime(dataSourceFactory,
                        configMerger,
                        extras,
                        injectedCayenneConfigs);

        shutdownManager.addShutdownHook(() -> {
            bootLogger.trace(() -> "shutting down Cayenne...");
            runtime.shutdown();
        });

        // TODO: listeners should be really contributable to Cayenne via DI,
        // just like filters...
        if (!listeners.isEmpty()) {
            DataDomain domain = runtime.getDataDomain();
            listeners.forEach(domain::addListener);
        }

        return runtime;
    }

    protected Collection<Module> extraCayenneModules(Set<Module> customModules, Set<DataChannelFilter> filters) {
        Collection<Module> extras = new ArrayList<>();
        extras.addAll(customModules);

        if (!filters.isEmpty()) {
            extras.add(cayenneBinder -> {
                ListBuilder<DataChannelFilter> listBinder = ServerModule.contributeDomainFilters(cayenneBinder);
                filters.forEach(listBinder::add);
            });
        }

        return extras;
    }
}
