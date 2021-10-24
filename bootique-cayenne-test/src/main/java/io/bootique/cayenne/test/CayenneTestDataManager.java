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

import io.bootique.BQRuntime;
import io.bootique.jdbc.test.Table;
import io.bootique.jdbc.test.TestDataManager;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Property;

/**
 *  @deprecated since 3.0.M1, as we are we phasing out JUnit 4 support in favor of JUnit 5
 */
@Deprecated
public class CayenneTestDataManager extends TestDataManager {

    private CayenneTableManager tableManager;
    private ServerRuntime runtime;
    private boolean refreshCayenneCaches;

    protected CayenneTestDataManager(
            ServerRuntime runtime,
            CayenneTableManager tableManager,
            boolean deleteData,
            boolean refreshCayenneCaches,
            Table... tablesInInsertOrder) {

        super(deleteData, tablesInInsertOrder);

        this.runtime = runtime;
        this.tableManager = tableManager;
        this.refreshCayenneCaches = refreshCayenneCaches;
    }

    /**
     * Creates a builder of CayenneTestDataManager.
     *
     * @param runtime {@link BQRuntime} used in the test.
     * @return a new instance of CayenneTestDataManager builder.
     */
    public static CayenneTestDataManagerBuilder builder(BQRuntime runtime) {
        return new CayenneTestDataManagerBuilder(runtime);
    }

    public static Table createTableModel(BQRuntime runtime, Class<?> entityType) {
        return CayenneModelUtils.createTableModel(runtime, entityType);
    }

    public static Table createTableModel(BQRuntime runtime, String tableName) {
        return CayenneModelUtils.createTableModel(runtime, tableName);
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        if (refreshCayenneCaches) {
            refreshCayenneCaches();
        }
    }

    public Table getTable(Class<?> entityType) {
        return tableManager.getTable(entityType);
    }

    /**
     * Returns a Table related to a given entity via the specified relationship. Useful for navigation to join tables
     * that are not directly mapped to Java classes.
     *
     * @param entityType
     * @param relationship
     * @param tableIndex   An index in a list of tables spanned by 'relationship'. Index of 0 corresponds to the target
     *                     DbEntity of the first object in a chain of DbRelationships for a given ObjRelationship.
     * @return a Table related to a given entity via the specified relationship.
     */
    public Table getRelatedTable(Class<?> entityType, Property<?> relationship, int tableIndex) {
        return tableManager.getRelatedTable(entityType, relationship, tableIndex);
    }

    /**
     * @param entityType
     * @param relationship
     * @return a Table related to a given entity via the specified relationship.
     */
    public Table getRelatedTable(Class<?> entityType, Property<?> relationship) {
        return tableManager.getRelatedTable(entityType, relationship, 0);
    }

    /**
     * @return Cayenne {@link ServerRuntime} underlying this data manager.
     */
    public ServerRuntime getRuntime() {
        return runtime;
    }

    public void refreshCayenneCaches() {

        DataDomain domain = runtime.getDataDomain();

        if (domain.getSharedSnapshotCache() != null) {
            domain.getSharedSnapshotCache().clear();
        }

        if (domain.getQueryCache() != null) {
            // note that this also flushes per-context caches .. at least with JCache implementation
            domain.getQueryCache().clear();
        }
    }

}
