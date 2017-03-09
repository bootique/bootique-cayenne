package io.bootique.cayenne;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ModuleExtender;
import io.bootique.cayenne.annotation.CayenneConfigs;
import io.bootique.cayenne.annotation.CayenneListener;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.di.Module;

/**
 * @since 0.19
 */
public class CayenneModuleExtender extends ModuleExtender<CayenneModuleExtender> {

    private Multibinder<DataChannelFilter> filters;
    private Multibinder<Object> listeners;
    private Multibinder<String> projects;
    private Multibinder<Module> modules;

    public CayenneModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneModuleExtender initAllExtensions() {
        contributeListeners();
        contributeFilters();
        contributeModules();
        contributeProjects();
        return this;
    }

    public CayenneModuleExtender addFilter(DataChannelFilter filter) {
        contributeFilters().addBinding().toInstance(filter);
        return this;
    }

    public CayenneModuleExtender addFilter(Class<? extends DataChannelFilter> filterType) {
        contributeFilters().addBinding().to(filterType);
        return this;
    }

    public CayenneModuleExtender addListener(Object listener) {
        contributeListeners().addBinding().toInstance(listener);
        return this;
    }

    public CayenneModuleExtender addListener(Class<?> listenerType) {
        contributeListeners().addBinding().to(listenerType);
        return this;
    }

    public CayenneModuleExtender addProject(String projectConfig) {
        contributeProjects().addBinding().toInstance(projectConfig);
        return this;
    }

    public CayenneModuleExtender addModule(Module module) {
        contributeModules().addBinding().toInstance(module);
        return this;
    }

    public CayenneModuleExtender addModule(Class<? extends Module> moduleType) {
        contributeModules().addBinding().to(moduleType);
        return this;
    }

    public CayenneModuleExtender addModule(Key<? extends Module> moduleKey) {
        contributeModules().addBinding().to(moduleKey);
        return this;
    }

    protected Multibinder<DataChannelFilter> contributeFilters() {
        return filters != null ? filters : (filters = newSet(DataChannelFilter.class));
    }

    protected Multibinder<Object> contributeListeners() {
        return listeners != null ? listeners : (listeners = newSet(Object.class, CayenneListener.class));
    }

    protected Multibinder<String> contributeProjects() {
        return projects != null ? projects : (projects = newSet(String.class, CayenneConfigs.class));
    }

    protected Multibinder<Module> contributeModules() {
        return modules != null ? modules : (modules = newSet(Module.class));
    }
}
