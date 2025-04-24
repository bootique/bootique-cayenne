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
import io.bootique.cayenne.v50.annotation.CayenneConfigs;
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

public class CayenneModuleExtender extends ModuleExtender<CayenneModuleExtender> {

    static final String COMMIT_LOG_ANNOTATION = CayenneModuleExtender.class.getPackageName() + ".commit_log_annotation";

    private SetBuilder<MappedDataChannelSyncFilter> syncFilters;
    private SetBuilder<MappedDataChannelSyncFilterType> syncFilterTypes;
    private SetBuilder<DataChannelQueryFilter> queryFilters;
    private SetBuilder<Object> listeners;
    private SetBuilder<String> projects;
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
        contributeProjects();
        contributeStartupListeners();
        contributeCommitLogListeners();
        contributeCommitLogListenerTypes();
        contributeExtendedTypes();
        contributeValueObjectTypes();
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addStartupListener(CayenneStartupListener listener) {
        contributeStartupListeners().addInstance(listener);
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addStartupListener(Class<? extends CayenneStartupListener> listenerType) {
        contributeStartupListeners().add(listenerType);
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addSyncFilter(DataChannelSyncFilter filter, boolean includeInTransaction) {
        contributeSyncFilters().addInstance(new MappedDataChannelSyncFilter(filter, includeInTransaction));
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addSyncFilter(Class<? extends DataChannelSyncFilter> filterType, boolean includeInTransaction) {
        contributeSyncFilterTypes().addInstance(new MappedDataChannelSyncFilterType(filterType, includeInTransaction));
        return this;
    }

    /**
     * @since 1.1
     */
    public CayenneModuleExtender addQueryFilter(DataChannelQueryFilter filter) {
        contributeQueryFilters().addInstance(filter);
        return this;
    }

    /**
     * @since 1.1
     */
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

    public CayenneModuleExtender addProject(String projectConfig) {
        contributeProjects().addInstance(projectConfig);
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

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addCommitLogListener(CommitLogListener listener, boolean includeInTransaction) {
        contributeCommitLogListeners().addInstance(new MappedCommitLogListener(listener, includeInTransaction, null));
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addCommitLogListener(CommitLogListener listener, boolean includeInTransaction, Class<? extends CommitLogListener> after) {
        contributeCommitLogListeners().addInstance(new MappedCommitLogListener(listener, includeInTransaction, after));
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addCommitLogListener(Class<? extends CommitLogListener> listenerType, boolean includeInTransaction) {
        contributeCommitLogListenerTypes().addInstance(new MappedCommitLogListenerType(listenerType, includeInTransaction, null));
        return this;
    }

    /**
     * @since 3.0
     */
    public CayenneModuleExtender addCommitLogListener(Class<? extends CommitLogListener> listenerType, boolean includeInTransaction, Class<? extends CommitLogListener> after) {
        contributeCommitLogListenerTypes().addInstance(new MappedCommitLogListenerType(listenerType, includeInTransaction, after));
        return this;
    }

    /**
     * Enables entity filtering and change event preprocessing for commit log events. If called, Cayenne will be
     * configured to respect {@link org.apache.cayenne.commitlog.CommitLog} annotation on entities. This annotation
     * allows to explicitly specify change tracking for a subset of entities, obfuscate confidential properties
     * (such as passwords), etc.
     *
     * @since 3.0
     */
    public CayenneModuleExtender applyCommitLogAnnotation() {
        binder.bind(Key.get(Boolean.class, COMMIT_LOG_ANNOTATION)).toInstance(Boolean.TRUE);
        return this;
    }

    /**
     * @since 3.0.M2
     */
    public CayenneModuleExtender addExtendedType(ExtendedType<?> type) {
        contributeExtendedTypes().addInstance(type);
        return this;
    }

    /**
     * @since 3.0.M2
     */
    public CayenneModuleExtender addExtendedType(Class<? extends ExtendedType<?>> type) {
        contributeExtendedTypes().add(type);
        return this;
    }

    /**
     * @since 3.0.M2
     */
    public CayenneModuleExtender addValueObjectType(ValueObjectType<?, ?> type) {
        contributeValueObjectTypes().addInstance(type);
        return this;
    }

    /**
     * @since 3.0.M2
     */
    public CayenneModuleExtender addValueObjectType(Class<? extends ValueObjectType<?, ?>> type) {
        contributeValueObjectTypes().add(type);
        return this;
    }

    protected SetBuilder<DataChannelQueryFilter> contributeQueryFilters() {
        return queryFilters != null ? queryFilters : (queryFilters = newSet(DataChannelQueryFilter.class));
    }

    protected SetBuilder<MappedDataChannelSyncFilter> contributeSyncFilters() {
        return syncFilters != null ? syncFilters : (syncFilters = newSet(MappedDataChannelSyncFilter.class));
    }

    protected SetBuilder<MappedDataChannelSyncFilterType> contributeSyncFilterTypes() {
        return syncFilterTypes != null ? syncFilterTypes : (syncFilterTypes = newSet(MappedDataChannelSyncFilterType.class));
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

    protected SetBuilder<CayenneStartupListener> contributeStartupListeners() {
        return startupListeners != null ? startupListeners : (startupListeners = newSet(CayenneStartupListener.class));
    }

    protected SetBuilder<MappedCommitLogListener> contributeCommitLogListeners() {
        return commitLogListeners != null ? commitLogListeners : (commitLogListeners = newSet(MappedCommitLogListener.class));
    }

    protected SetBuilder<MappedCommitLogListenerType> contributeCommitLogListenerTypes() {
        return commitLogListenerTypes != null ? commitLogListenerTypes : (commitLogListenerTypes = newSet(MappedCommitLogListenerType.class));
    }

    protected SetBuilder<ExtendedType> contributeExtendedTypes() {
        return extendedType != null ? extendedType : (extendedType = newSet(ExtendedType.class));
    }

    protected SetBuilder<ValueObjectType> contributeValueObjectTypes() {
        return valueObjectTypes != null ? valueObjectTypes : (valueObjectTypes = newSet(ValueObjectType.class));
    }
}
