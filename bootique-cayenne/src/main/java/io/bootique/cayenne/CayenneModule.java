package io.bootique.cayenne;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ConfigModule;
import io.bootique.cayenne.annotation.CayenneListener;
import io.bootique.cayenne.annotation.CayenneConfigs;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jdbc.DataSourceFactory;
import io.bootique.log.BootLogger;
import io.bootique.shutdown.ShutdownManager;
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
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneModuleExtender} that can be used to load Cayenne custom extensions.
     * @since 0.19
     */
    public static CayenneModuleExtender extend(Binder binder) {
        return new CayenneModuleExtender(binder);
    }

    /**
     * Returns a Guice {@link Multibinder} to add Cayenne DataChannelFilters.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for Cayenne DataChannelFilters
     * @since 0.13
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneModuleExtender#addFilter(Class)} or similar methods.
     */
    @Deprecated
    public static Multibinder<DataChannelFilter> contributeFilters(Binder binder) {
        return Multibinder.newSetBinder(binder, DataChannelFilter.class);
    }

    /**
     * Returns a Guice {@link Multibinder} to add Cayenne annotated listeners.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for Cayenne annotated listeners.
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneModuleExtender#addListener(Class)} or similar methods.
     */
    @Deprecated
    public static Multibinder<Object> contributeListeners(Binder binder) {
        return Multibinder.newSetBinder(binder, Object.class, CayenneListener.class);
    }

    /**
     * Returns a Guice {@link Multibinder} to add Cayenne project configs.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for Cayenne project configs.
     * @since 0.18
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneModuleExtender#addProject(String)}.
     */
    @Deprecated
    public static Multibinder<String> contributeProjects(Binder binder) {
        return Multibinder.newSetBinder(binder, String.class, CayenneConfigs.class);
    }

    /**
     * Returns a Guice {@link Multibinder} to add custom Cayenne DI modules.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return returns a {@link Multibinder} for Cayenne DI modules.
     * @since 0.17
     * @deprecated since 0.19 call {@link #extend(Binder)} and then call
     * {@link CayenneModuleExtender#addModule(Class)} or similar methods.
     */
    @Deprecated
    public static Multibinder<Module> contributeModules(Binder binder) {
        return Multibinder.newSetBinder(binder, Module.class);
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
        ServerRuntime runtime = configFactory.config(ServerRuntimeFactory.class, configPrefix)
                .createCayenneRuntime(dataSourceFactory, extras,
                        cayenneConfigs -> configMerger.merge(cayenneConfigs, injectedCayenneConfigs));

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
        extras.add(new CayenneJava8Module());
        extras.addAll(customModules);

        if (!filters.isEmpty()) {
            extras.add(cayenneBinder -> {
                ListBuilder<DataChannelFilter> listBinder = cayenneBinder
                        .bindList(Constants.SERVER_DOMAIN_FILTERS_LIST);
                filters.forEach(listBinder::add);
            });
        }

        return extras;
    }
}
