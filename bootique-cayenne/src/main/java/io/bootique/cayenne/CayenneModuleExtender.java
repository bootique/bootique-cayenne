/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.cayenne;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ModuleExtender;
import io.bootique.cayenne.annotation.CayenneConfigs;
import io.bootique.cayenne.annotation.CayenneListener;
import io.bootique.jdbc.managed.ManagedDataSourceFactory;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;

import java.lang.reflect.Type;

/**
 * @since 0.19
 */
public class CayenneModuleExtender extends ModuleExtender<CayenneModuleExtender> {

    private static final Key<Class<? extends DataDomainProvider>> DOMAIN_PROVIDER = Key
            .get(new TypeLiteral<Class<? extends DataDomainProvider>>() {
            });

    private Multibinder<DataChannelFilter> filters;
    private Multibinder<Object> listeners;
    private Multibinder<String> projects;
    private Multibinder<Module> modules;
    private Multibinder<Class<? extends DataDomainProvider>> dataDomainProviders;

    public CayenneModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneModuleExtender initAllExtensions() {
        contributeListeners();
        contributeFilters();
        contributeModules();
        contributeProjects();
        contributeDataDomainProvider();

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

    /**
     * @since 0.26
     */
    public CayenneModuleExtender addDataDomainProvider(Class<? extends DataDomainProvider> providerClass) {
        contributeDataDomainProvider().addBinding().toInstance(providerClass);
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

    /**
     * @since 0.26
     */
    protected Multibinder<Class<? extends DataDomainProvider>> contributeDataDomainProvider() {
        return dataDomainProviders != null ? dataDomainProviders : (dataDomainProviders = newSet(DOMAIN_PROVIDER));
    }
}
