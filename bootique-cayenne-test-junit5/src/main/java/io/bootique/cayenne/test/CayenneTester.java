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
package io.bootique.cayenne.test;

import io.bootique.cayenne.test.tester.CayenneTesterBootiqueHook;
import io.bootique.cayenne.test.tester.FilteredDataMap;
import io.bootique.cayenne.test.tester.SchemaGenerator;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A JUnit5 extension that manages test schema, data and Cayenne runtime state between the tests.
 *
 * @since 2.0
 */
public class CayenneTester implements BeforeAllCallback, BeforeEachCallback {

    private boolean refreshCayenneCaches;
    private boolean deleteBeforeEachTest;
    private Collection<Class<? extends Persistent>> entities;

    private CayenneTesterBootiqueHook bootiqueHook;
    private Map<String, DataMap> managedEntitiesByNode;

    public static CayenneTester create() {
        return new CayenneTester();
    }

    protected CayenneTester() {
        this.bootiqueHook = new CayenneTesterBootiqueHook();
        this.refreshCayenneCaches = true;
        this.deleteBeforeEachTest = false;
        this.entities = new ArrayList<>();
    }

    public CayenneTester doNoRefreshCayenneCaches() {
        this.refreshCayenneCaches = false;
        return this;
    }

    /**
     * Configures the Tester to manage a subset of entities out a potentially very large number of entities in the model.
     *
     * @param entities a list of entities to manage (create schema for, delete test data, etc.)
     * @return this tester
     */
    public CayenneTester entities(Class<? extends Persistent>... entities) {
        for (Class<? extends Persistent> e : entities) {
            this.entities.add(e);
        }

        return this;
    }

    /**
     * Configures the Tester to delete data from the tester's entity classes before each test.
     *
     * @return this tester
     */
    public CayenneTester deleteBeforeEachTest() {
        this.deleteBeforeEachTest = true;
        return this;
    }

    /**
     * Returns a a new Bootique module that registers Cayenne test extensions that allow the tester to interact
     * with Bootique runtime defined somewhere in the test.
     *
     * @return a new Bootique module that registers Cayenne test extensions
     */
    public BQModule registerTestHooks() {
        return this::configure;
    }

    protected void configure(Binder binder) {
        binder.bind(CayenneTesterBootiqueHook.class).toInstance(bootiqueHook);
    }

    protected DataDomain getDomain() {
        return bootiqueHook.getRuntime().getDataDomain();
    }

    public ServerRuntime getRuntime() {
        return bootiqueHook.getRuntime();
    }

    protected Map<String, DataMap> getManagedEntitiesByNode() {
        assertNotNull(managedEntitiesByNode, "Managed Cayenne entities are not resolved. Called outside of test lifecycle?");
        return managedEntitiesByNode;
    }

    public String getTableName(Class<? extends Persistent> entity) {
        ObjEntity e = getDomain().getEntityResolver().getObjEntity(entity);
        if (e == null) {
            throw new IllegalStateException("Type is not mapped in Cayenne: " + entity);
        }

        return e.getDbEntity().getName();
    }

    protected void triggerCayenneStackInit() {

    }

    protected void resolveManagedEntitiesByNode() {

        DataDomain domain = getDomain();
        Map<String, Map<String, DbEntity>> byNode = new HashMap<>();

        if (!entities.isEmpty()) {
            EntityResolver resolver = domain.getEntityResolver();

            entities.forEach(t -> {

                ObjEntity e = resolver.getObjEntity(t);
                if (e == null) {
                    throw new IllegalStateException("Type is not mapped in Cayenne: " + t);
                }

                DbEntity dbe = e.getDbEntity();
                DataNode node = domain.lookupDataNode(dbe.getDataMap());
                byNode.computeIfAbsent(node.getName(), nn -> new HashMap<>()).put(dbe.getName(), dbe);
            });
        }

        Map<String, DataMap> managedEntitiesByNode = new HashMap<>();
        byNode.forEach((k, v) -> managedEntitiesByNode.put(k, new FilteredDataMap("CayenneTester_" + k, v)));
        this.managedEntitiesByNode = managedEntitiesByNode;
    }

    protected void initSchema() {
        SchemaGenerator generator = new SchemaGenerator(getDomain());
        getManagedEntitiesByNode().forEach(generator::createSchema);
    }

    protected void deleteTestData() {
        throw new UnsupportedOperationException("TODO");
    }

    protected void refreshCayenneCaches() {

        DataDomain domain = getDomain();

        if (domain.getSharedSnapshotCache() != null) {
            domain.getSharedSnapshotCache().clear();
        }

        if (domain.getQueryCache() != null) {
            // note that this also flushes per-context caches .. at least with JCache implementation
            domain.getQueryCache().clear();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        resolveManagedEntitiesByNode();
        initSchema();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (refreshCayenneCaches) {
            refreshCayenneCaches();
        }

        if (deleteBeforeEachTest && !entities.isEmpty()) {
            deleteTestData();
        }
    }
}
