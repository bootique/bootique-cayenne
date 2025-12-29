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

package io.bootique.cayenne.v50;

import io.bootique.ModuleExtender;
import io.bootique.cayenne.v50.annotation.CayenneListener;
import io.bootique.cayenne.v50.commitlog.MappedCommitLogListener;
import io.bootique.cayenne.v50.commitlog.MappedCommitLogListenerType;
import io.bootique.cayenne.v50.syncfilter.MappedDataChannelSyncFilter;
import io.bootique.cayenne.v50.syncfilter.MappedDataChannelSyncFilterType;
import io.bootique.di.Binder;
import io.bootique.di.Key;
import io.bootique.di.SetBuilder;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.di.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.0
 */
public class CayenneModuleExtender extends ModuleExtender<CayenneModuleExtender> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CayenneModuleExtender.class);

    static final String COMMIT_LOG_ANNOTATION = CayenneModuleExtender.class.getPackageName() + ".commit_log_annotation";

    private SetBuilder<MappedDataChannelSyncFilter> syncFilters;
    private SetBuilder<MappedDataChannelSyncFilterType> syncFilterTypes;
    private SetBuilder<DataChannelQueryFilter> queryFilters;
    private SetBuilder<Object> listeners;
    private SetBuilder<String> locations;
    private SetBuilder<Module> modules;
    private SetBuilder<CayenneStartupListener> startupListeners;
    private SetBuilder<MappedCommitLogListener> commitLogListeners;
    private SetBuilder<MappedCommitLogListenerType> commitLogListenerTypes;
    private SetBuilder<ExtendedType> extendedType;
    private SetBuilder<ValueObjectType> valueObjectTypes;

    public CayenneModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneModuleExtender initAllExtensions() {
        contributeListeners();
        contributeQueryFilters();
        contributeSyncFilters();
        contributeSyncFilterTypes();
        contributeModules();
        contributeLocations();
        contributeStartupListeners();
        contributeCommitLogListeners();
        contributeCommitLogListenerTypes();
        contributeExtendedTypes();
        contributeValueObjectTypes();
        return this;
    }

    public CayenneModuleExtender addStartupListener(CayenneStartupListener listener) {
        contributeStartupListeners().addInstance(listener);
        return this;
    }

    public CayenneModuleExtender addStartupListener(Class<? extends CayenneStartupListener> listenerType) {
        contributeStartupListeners().add(listenerType);
        return this;
    }

    public CayenneModuleExtender addSyncFilter(DataChannelSyncFilter filter, boolean includeInTransaction) {
        contributeSyncFilters().addInstance(new MappedDataChannelSyncFilter(filter, includeInTransaction));
        return this;
    }

    public CayenneModuleExtender addSyncFilter(Class<? extends DataChannelSyncFilter> filterType, boolean includeInTransaction) {
        contributeSyncFilterTypes().addInstance(new MappedDataChannelSyncFilterType(filterType, includeInTransaction));
        return this;
    }

    public CayenneModuleExtender addQueryFilter(DataChannelQueryFilter filter) {
        contributeQueryFilters().addInstance(filter);
        return this;
    }

    public CayenneModuleExtender addQueryFilter(Class<? extends DataChannelQueryFilter> filterType) {
        contributeQueryFilters().add(filterType);
        return this;
    }

    public CayenneModuleExtender addListener(Object listener) {
        contributeListeners().addInstance(listener);
        return this;
    }

    public CayenneModuleExtender addListener(Class<?> listenerType) {
        contributeListeners().add(listenerType);
        return this;
    }

    /**
     * @deprecated in favor of {@link #addLocation(String)} with "classpath:" or "cayenne.locations" configuration.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public CayenneModuleExtender addProject(String projectConfig) {
        String cpPrefix = "classpath:";
        String uri = projectConfig.startsWith(cpPrefix) ? projectConfig : cpPrefix + projectConfig;

        LOGGER.warn("""
                ** Adding a project via deprecated API. Use addLocation("{}") instead""", uri);
        return addLocation(uri);
    }

    /**
     * Adds Cayenne project location, using the Bootique resource format. The locations added here will be merged with
     * any extra locations added via configuration.
     *
     * @since 4.0
     */
    public CayenneModuleExtender addLocation(String projectLocation) {
        // Note that we can't simply stick it in "bq.cayenne.locations[.length]" property, as there can be multiple
        // projects
        contributeLocations().addInstance(projectLocation);
        return this;
    }

    public CayenneModuleExtender addModule(Module module) {
        contributeModules().addInstance(module);
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

    public CayenneModuleExtender addCommitLogListener(CommitLogListener listener, boolean includeInTransaction) {
        contributeCommitLogListeners().addInstance(new MappedCommitLogListener(listener, includeInTransaction, null));
        return this;
    }

    public CayenneModuleExtender addCommitLogListener(CommitLogListener listener, boolean includeInTransaction, Class<? extends CommitLogListener> after) {
        contributeCommitLogListeners().addInstance(new MappedCommitLogListener(listener, includeInTransaction, after));
        return this;
    }

    public CayenneModuleExtender addCommitLogListener(Class<? extends CommitLogListener> listenerType, boolean includeInTransaction) {
        contributeCommitLogListenerTypes().addInstance(new MappedCommitLogListenerType(listenerType, includeInTransaction, null));
        return this;
    }

    public CayenneModuleExtender addCommitLogListener(Class<? extends CommitLogListener> listenerType, boolean includeInTransaction, Class<? extends CommitLogListener> after) {
        contributeCommitLogListenerTypes().addInstance(new MappedCommitLogListenerType(listenerType, includeInTransaction, after));
        return this;
    }

    /**
     * Enables entity filtering and change event preprocessing for commit log events. If called, Cayenne will be
     * configured to respect {@link org.apache.cayenne.commitlog.CommitLog} annotation on entities. This annotation
     * allows to explicitly specify change tracking for a subset of entities, obfuscate confidential properties
     * (such as passwords), etc.
     */
    public CayenneModuleExtender applyCommitLogAnnotation() {
        binder.bind(Key.get(Boolean.class, COMMIT_LOG_ANNOTATION)).toInstance(Boolean.TRUE);
        return this;
    }

    public CayenneModuleExtender addExtendedType(ExtendedType<?> type) {
        contributeExtendedTypes().addInstance(type);
        return this;
    }

    public CayenneModuleExtender addExtendedType(Class<? extends ExtendedType<?>> type) {
        contributeExtendedTypes().add(type);
        return this;
    }

    public CayenneModuleExtender addValueObjectType(ValueObjectType<?, ?> type) {
        contributeValueObjectTypes().addInstance(type);
        return this;
    }

    public CayenneModuleExtender addValueObjectType(Class<? extends ValueObjectType<?, ?>> type) {
        contributeValueObjectTypes().add(type);
        return this;
    }

    SetBuilder<DataChannelQueryFilter> contributeQueryFilters() {
        return queryFilters != null ? queryFilters : (queryFilters = newSet(DataChannelQueryFilter.class));
    }

    SetBuilder<MappedDataChannelSyncFilter> contributeSyncFilters() {
        return syncFilters != null ? syncFilters : (syncFilters = newSet(MappedDataChannelSyncFilter.class));
    }

    SetBuilder<MappedDataChannelSyncFilterType> contributeSyncFilterTypes() {
        return syncFilterTypes != null ? syncFilterTypes : (syncFilterTypes = newSet(MappedDataChannelSyncFilterType.class));
    }

    SetBuilder<Object> contributeListeners() {
        return listeners != null ? listeners : (listeners = newSet(Object.class, CayenneListener.class));
    }

    SetBuilder<String> contributeLocations() {
        return locations != null ? locations : (locations = newSet(String.class, CayenneModule.LOCATIONS_BINDING));
    }

    SetBuilder<Module> contributeModules() {
        return modules != null ? modules : (modules = newSet(Module.class));
    }

    SetBuilder<CayenneStartupListener> contributeStartupListeners() {
        return startupListeners != null ? startupListeners : (startupListeners = newSet(CayenneStartupListener.class));
    }

    SetBuilder<MappedCommitLogListener> contributeCommitLogListeners() {
        return commitLogListeners != null ? commitLogListeners : (commitLogListeners = newSet(MappedCommitLogListener.class));
    }

    SetBuilder<MappedCommitLogListenerType> contributeCommitLogListenerTypes() {
        return commitLogListenerTypes != null ? commitLogListenerTypes : (commitLogListenerTypes = newSet(MappedCommitLogListenerType.class));
    }

    SetBuilder<ExtendedType> contributeExtendedTypes() {
        return extendedType != null ? extendedType : (extendedType = newSet(ExtendedType.class));
    }

    SetBuilder<ValueObjectType> contributeValueObjectTypes() {
        return valueObjectTypes != null ? valueObjectTypes : (valueObjectTypes = newSet(ValueObjectType.class));
    }
}
