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

package io.bootique.cayenne.v41;

import io.bootique.ModuleExtender;
import io.bootique.cayenne.v41.annotation.CayenneConfigs;
import io.bootique.cayenne.v41.annotation.CayenneListener;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.SetBuilder;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.di.Module;

/**
 * @since 1.0.RC1
 */
public class CayenneModuleExtender extends ModuleExtender<CayenneModuleExtender> {

    private SetBuilder<DataChannelFilter> filters;
    private SetBuilder<DataChannelSyncFilter> syncFilters;
    private SetBuilder<DataChannelQueryFilter> queryFilters;
    private SetBuilder<Object> listeners;
    private SetBuilder<String> projects;
    private SetBuilder<Module> modules;

    public CayenneModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneModuleExtender initAllExtensions() {
        contributeListeners();
        contributeFilters();
        contributeQueryFilters();
        contributeSyncFilters();
        contributeModules();
        contributeProjects();
        return this;
    }

    /**
     * @since 1.1
     */
    public CayenneModuleExtender addSyncFilter(DataChannelSyncFilter filter) {
        contributeSyncFilters().add(filter);
        return this;
    }

    /**
     * @since 1.1
     */
    public CayenneModuleExtender addSyncFilter(Class<? extends DataChannelSyncFilter> filterType) {
        contributeSyncFilters().add(filterType);
        return this;
    }

    /**
     * @since 1.1
     */
    public CayenneModuleExtender addQueryFilter(DataChannelQueryFilter filter) {
        contributeQueryFilters().add(filter);
        return this;
    }

    /**
     * @since 1.1
     */
    public CayenneModuleExtender addQueryFilter(Class<? extends DataChannelQueryFilter> filterType) {
        contributeQueryFilters().add(filterType);
        return this;
    }

    /**
     * @deprecated since 1.1 as Cayenne DataChannelFilter is deprecated. Consider migrating your filters to
     * DataChannelSyncFilter and DataChannelQueryFilter and use {@link #addSyncFilter(DataChannelSyncFilter)},
     * {@link #addQueryFilter(DataChannelQueryFilter)}.
     */
    @Deprecated
    public CayenneModuleExtender addFilter(DataChannelFilter filter) {
        contributeFilters().add(filter);
        return this;
    }

    /**
     * @deprecated since 1.1 as Cayenne DataChannelFilter is deprecated. Consider migrating your filters to
     * DataChannelSyncFilter and DataChannelQueryFilter and use {@link #addSyncFilter(Class)},
     * {@link #addQueryFilter(Class)}.
     */
    @Deprecated
    public CayenneModuleExtender addFilter(Class<? extends DataChannelFilter> filterType) {
        contributeFilters().add(filterType);
        return this;
    }

    public CayenneModuleExtender addListener(Object listener) {
        contributeListeners().add(listener);
        return this;
    }

    public CayenneModuleExtender addListener(Class<?> listenerType) {
        contributeListeners().add(listenerType);
        return this;
    }

    public CayenneModuleExtender addProject(String projectConfig) {
        contributeProjects().add(projectConfig);
        return this;
    }

    public CayenneModuleExtender addModule(Module module) {
        contributeModules().add(module);
        return this;
    }

    public CayenneModuleExtender addModule(Class<? extends Module> moduleType) {
        contributeModules().add(moduleType);
        return this;
    }

    public CayenneModuleExtender addModule(Key<? extends Module> moduleKey) {
        contributeModules().add(moduleKey);
        return this;
    }

    protected SetBuilder<DataChannelQueryFilter> contributeQueryFilters() {
        return queryFilters != null ? queryFilters : (queryFilters = newSet(DataChannelQueryFilter.class));
    }

    protected SetBuilder<DataChannelSyncFilter> contributeSyncFilters() {
        return syncFilters != null ? syncFilters : (syncFilters = newSet(DataChannelSyncFilter.class));
    }

    @Deprecated
    protected SetBuilder<DataChannelFilter> contributeFilters() {
        return filters != null ? filters : (filters = newSet(DataChannelFilter.class));
    }

    protected SetBuilder<Object> contributeListeners() {
        return listeners != null ? listeners : (listeners = newSet(Object.class, CayenneListener.class));
    }

    protected SetBuilder<String> contributeProjects() {
        return projects != null ? projects : (projects = newSet(String.class, CayenneConfigs.class));
    }

    protected SetBuilder<Module> contributeModules() {
        return modules != null ? modules : (modules = newSet(Module.class));
    }
}
