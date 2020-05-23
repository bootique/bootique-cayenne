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
package io.bootique.cayenne.test.tester;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.access.util.DoNothingOperationObserver;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.SQLTemplate;

import java.util.*;

/**
 * Manages various aspects of Cayenne stack (caching, schema generation, etc) for a subset of selected entities.
 *
 * @since 2.0
 */
public class CayenneRuntimeManager {

    private DataDomain domain;
    private Map<String, FilteredDataMap> managedEntitiesByNode;

    public static CayenneRuntimeManagerBuilder builder(DataDomain domain) {
        return new CayenneRuntimeManagerBuilder(domain);
    }

    protected CayenneRuntimeManager(DataDomain domain, Map<String, FilteredDataMap> managedEntitiesByNode) {
        this.domain = domain;
        this.managedEntitiesByNode = managedEntitiesByNode;
    }

    public void refreshCaches() {
        if (domain.getSharedSnapshotCache() != null) {
            domain.getSharedSnapshotCache().clear();
        }

        if (domain.getQueryCache() != null) {
            // note that this also flushes per-context caches .. at least with JCache implementation
            domain.getQueryCache().clear();
        }
    }

    // keeping public for the tests
    public Map<String, FilteredDataMap> getManagedEntitiesByNode() {
        return managedEntitiesByNode;
    }

    public void deleteData() {
        managedEntitiesByNode.forEach(this::deleteData);
    }

    public void createSchema() {
        managedEntitiesByNode.forEach(this::createSchema);
    }

    protected void deleteData(String nodeName, FilteredDataMap map) {

        DataNode node = domain.getDataNode(nodeName);

        // TODO: single transaction?
        map.getEntitiesInDeleteOrder().forEach(e -> deleteData(node, e));
    }

    protected void deleteData(DataNode node, DbEntity entity) {

        String name = node.getAdapter().getQuotingStrategy().quotedFullyQualifiedName(entity);
        String sql = "delete from " + name;

        // using SQLTemplate instead of SQLExec, as it can be executed directly on the DataNode
        SQLTemplate query = new SQLTemplate();
        query.setDefaultTemplate(sql);
        node.performQueries(Collections.singleton(query), new DoNothingOperationObserver());
    }

    protected void createSchema(String nodeName, DataMap map) {

        DataNode node = domain.getDataNode(nodeName);

        DbGenerator generator = new DbGenerator(node.getAdapter(), map, node.getJdbcEventLogger());
        generator.setShouldCreateTables(true);
        generator.setShouldDropTables(false);
        generator.setShouldCreateFKConstraints(true);
        generator.setShouldCreatePKSupport(true);
        generator.setShouldDropPKSupport(false);

        try {
            generator.runGenerator(node.getDataSource());
        } catch (Exception e) {
            throw new RuntimeException("Error creating schema for DataNode: " + node.getName(), e);
        }
    }
}
