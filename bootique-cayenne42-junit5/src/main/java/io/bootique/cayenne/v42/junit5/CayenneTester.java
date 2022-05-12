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
package io.bootique.cayenne.v42.junit5;

import io.bootique.cayenne.v42.CayenneModule;
import io.bootique.cayenne.v42.junit5.tester.*;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQBeforeMethodCallback;
import io.bootique.junit5.scope.BQBeforeScopeCallback;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.RelationshipProperty;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * A JUnit5 extension that manages test schema, data and Cayenne runtime state between tests. A single CayenneTester
 * can be used with a single {@link io.bootique.BQRuntime}. If you have multiple BQRuntimes in a test, you will need to
 * declare a separate CayenneTester for each one of them.
 *
 * @since 2.0
 */
public class CayenneTester implements BQBeforeScopeCallback, BQBeforeMethodCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(CayenneTester.class);

    private boolean refreshCayenneCaches;
    private boolean deleteBeforeEachTest;
    private boolean skipSchemaCreation;
    private Collection<Class<? extends Persistent>> entities;
    private Collection<Class<? extends Persistent>> entityGraphRoots;
    private boolean allTables;
    private Collection<String> tables;
    private Collection<String> tableGraphRoots;
    private Collection<RelatedEntity> relatedTables;

    private CayenneTesterBootiqueHook bootiqueHook;
    private CayenneRuntimeManager runtimeManager;
    private boolean unattached;
    private CommitCounter commitCounter;
    private QueryCounter queryCounter;

    public static CayenneTester create() {
        return new CayenneTester();
    }

    protected CayenneTester() {

        this.bootiqueHook = new CayenneTesterBootiqueHook()
                .onInit(r -> resolveRuntimeManager(r))
                .onInit(r -> createSchema());

        this.refreshCayenneCaches = true;
        this.deleteBeforeEachTest = false;
        this.skipSchemaCreation = false;
        this.commitCounter = new CommitCounter();
        this.queryCounter = new QueryCounter();
        this.unattached = true;
    }

    /**
     * @since 2.0.B1
     */
    public CayenneTester onInit(Consumer<ServerRuntime> callback) {
        bootiqueHook.onInit(callback);
        return this;
    }

    public CayenneTester doNoRefreshCayenneCaches() {
        this.refreshCayenneCaches = false;
        return this;
    }

    public CayenneTester skipSchemaCreation() {
        this.skipSchemaCreation = true;
        return this;
    }

    /**
     * @since 2.0.B1
     */
    public final CayenneTester allTables() {
        this.allTables = true;
        return this;
    }

    /**
     * Configures the Tester to manage a subset of entities out a potentially very large number of entities in the model.
     *
     * @param entities a list of entities to manage (create schema for, delete test data, etc.)
     * @return this tester
     */
    @SafeVarargs
    public final CayenneTester entities(Class<? extends Persistent>... entities) {

        if (this.entities == null) {
            this.entities = new HashSet<>();
        }

        Collections.addAll(this.entities, entities);
        return this;
    }

    @SafeVarargs
    public final CayenneTester entitiesAndDependencies(Class<? extends Persistent>... entities) {
        if (this.entityGraphRoots == null) {
            this.entityGraphRoots = new HashSet<>();
        }

        Collections.addAll(this.entityGraphRoots, entities);
        return this;
    }

    public CayenneTester tables(String... tables) {

        if (this.tables == null) {
            this.tables = new HashSet<>();
        }

        Collections.addAll(this.tables, tables);
        return this;
    }

    public CayenneTester tablesAndDependencies(String... tables) {

        if (this.tableGraphRoots == null) {
            this.tableGraphRoots = new HashSet<>();
        }

        Collections.addAll(this.tableGraphRoots, tables);
        return this;
    }

    public CayenneTester relatedTables(Class<? extends Persistent> entityType, Property<?> relationship) {

        if (this.relatedTables == null) {
            this.relatedTables = new HashSet<>();
        }

        this.relatedTables.add(new RelatedEntity(entityType, relationship.getName()));
        return this;
    }

    /**
     * Configures the Tester to delete data corresponding to the tester's entity model before each test.
     *
     * @return this tester
     */
    public CayenneTester deleteBeforeEachTest() {
        this.deleteBeforeEachTest = true;
        return this;
    }

    /**
     * Returns a new Bootique module that registers Cayenne test extensions. This module should be passed to a single
     * test {@link io.bootique.BQRuntime} to associate the tester with that runtime. This would allow the tester to
     * interact with Cayenne stack inside that runtime to perform its functions through the test lifecycle.
     *
     * @return a new Bootique module that registers Cayenne test extensions
     */
    public BQModule moduleWithTestHooks() {
        return this::configure;
    }

    protected void configure(Binder binder) {
        CayenneModule.extend(binder).addSyncFilter(commitCounter).addQueryFilter(queryCounter);

        binder.bind(CayenneTesterBootiqueHook.class)
                // wrapping the hook in provider to be able to run the checks for when this tester is erroneously
                // used for multiple runtimes
                .toProviderInstance(new CayenneTesterBootiqueHookProvider(bootiqueHook))
                // using "initOnStartup" to cause immediate Cayenne initialization. Any downsides?
                .initOnStartup();
    }

    public ServerRuntime getRuntime() {
        return bootiqueHook.getRuntime();
    }

    protected CayenneRuntimeManager getRuntimeManager() {
        Assertions.assertNotNull(runtimeManager, "Cayenne runtime is not resolved. Called outside of test lifecycle?");
        return runtimeManager;
    }

    public String getTableName(Class<? extends Persistent> entity) {
        ObjEntity e = getRuntime().getDataDomain().getEntityResolver().getObjEntity(entity);
        if (e == null) {
            throw new IllegalStateException("Type is not mapped in Cayenne: " + entity);
        }

        return e.getDbEntity().getName();
    }

    /**
     * Returns a name of a table related to a given entity via the specified relationship. Useful for navigation to
     * join tables that are not directly mapped to Java classes.
     *
     * @param entity
     * @param relationship
     * @param tableIndex   An index in a list of tables spanned by 'relationship'. Index of 0 corresponds to the target
     *                     DbEntity of the first object in a chain of DbRelationships for a given ObjRelationship.
     * @return a name of a table related to a given entity via the specified relationship.
     * @since 2.0.B1
     */
    public String getRelatedTableName(Class<? extends Persistent> entity, RelationshipProperty<?> relationship, int tableIndex) {
        EntityResolver entityResolver = getRuntime().getDataDomain().getEntityResolver();
        return new RelatedEntity(entity, relationship.getName()).getRelatedTable(entityResolver, tableIndex).getName();
    }

    /**
     * Checks whether Cayenne performed the expected number of DB commits within a single test method.
     */
    public void assertCommitCount(int expected) {
        commitCounter.assertCount(expected);
    }

    /**
     * Checks whether Cayenne performed the expected number of DB queries within a single test method.
     *
     * @since 2.0.B1
     */
    public void assertQueryCount(int expected) {
        queryCounter.assertCount(expected);
    }

    protected void resolveRuntimeManager(ServerRuntime runtime) {

        if (runtime == null) {
            LOGGER.warn("CayenneTester is not attached to a test app. "
                    + "To take advantage of CayenneTester, pass the module "
                    + "produced via 'moduleWithTestHooks' when assembling a test BQRuntime.");
            this.unattached = true;
            return;
        }

        this.unattached = false;

        if (allTables) {
            this.runtimeManager = CayenneRuntimeManager
                    .builder(runtime.getDataDomain())
                    .dbEntities(runtime.getDataDomain().getEntityResolver().getDbEntities())
                    .build();
        } else {

            this.runtimeManager = CayenneRuntimeManager
                    .builder(runtime.getDataDomain())
                    .entities(entities)
                    .entityGraphRoots(entityGraphRoots)
                    .tables(tables)
                    .tableGraphRoots(tableGraphRoots)
                    .relatedEntities(relatedTables)
                    .build();
        }
    }

    protected void createSchema() {
        if (skipSchemaCreation) {
            return;
        }

        if (unattached) {
            LOGGER.warn("Won't create DB schema. CayenneTester is not attached to a test app");
            return;
        }

        getRuntimeManager().createSchema();
    }

    @Override
    public void beforeScope(BQTestScope scope, ExtensionContext context) {
        bootiqueHook.initIfNeeded();
    }

    @Override
    public void beforeMethod(BQTestScope scope, ExtensionContext context) {

        if (unattached) {
            return;
        }

        if (refreshCayenneCaches) {
            getRuntimeManager().refreshCaches();
        }

        if (deleteBeforeEachTest) {
            getRuntimeManager().deleteData();
        }

        commitCounter.reset();
        queryCounter.reset();
    }
}
