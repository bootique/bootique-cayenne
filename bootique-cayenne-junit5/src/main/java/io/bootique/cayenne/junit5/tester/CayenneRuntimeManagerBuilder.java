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
package io.bootique.cayenne.junit5.tester;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.*;

import java.util.*;

/**
 * @since 2.0
 */
public class CayenneRuntimeManagerBuilder {

    private DataDomain domain;

    // these two entity buckets are resolved independently and then intersected
    private Set<DbEntity> entities;
    private Set<DbEntity> entityGraphs;

    protected CayenneRuntimeManagerBuilder(DataDomain domain) {
        this.domain = domain;
        this.entities = new HashSet<>();
        this.entityGraphs = new HashSet<>();
    }

    public CayenneRuntimeManagerBuilder entities(Collection<Class<? extends Persistent>> entities) {
        if (entities != null) {
            entities.forEach(e -> resolve(this.entities, e));
        }

        return this;
    }

    public CayenneRuntimeManagerBuilder entityGraphRoots(Collection<Class<? extends Persistent>> entityGraphRoots) {
        if (entityGraphRoots != null) {
            entityGraphRoots.forEach(e -> resolveGraph(this.entityGraphs, e));
        }

        return this;
    }

    public CayenneRuntimeManagerBuilder tables(Collection<String> tables) {
        if (tables != null) {
            tables.forEach(t -> resolve(this.entities, t));
        }

        return this;
    }

    public CayenneRuntimeManagerBuilder tableGraphRoots(Collection<String> tableGraphRoots) {
        if (tableGraphRoots != null) {
            tableGraphRoots.forEach(t -> resolveGraph(this.entityGraphs, t));
        }

        return this;
    }

    public CayenneRuntimeManagerBuilder relatedEntities(Collection<RelatedEntity> relatedEntities) {
        if (relatedEntities != null) {
            relatedEntities.forEach(t -> resolve(this.entities, t));
        }
        return this;
    }

    public CayenneRuntimeManager build() {

        // using LinkedHashMap to preserve insert order of entities
        Map<String, LinkedHashMap<String, DbEntity>> byNode = new HashMap<>();
        buildEntitiesInInsertOrder().forEach(e -> {
            DataNode node = domain.lookupDataNode(e.getDataMap());
            byNode.computeIfAbsent(node.getName(), nn -> new LinkedHashMap<>()).put(e.getName(), e);
        });

        Map<String, FilteredDataMap> managedEntitiesByNode = new HashMap<>();
        byNode.forEach((k, v) -> managedEntitiesByNode.put(k, new FilteredDataMap("CayenneTester_" + k, v)));
        return new CayenneRuntimeManager(domain, managedEntitiesByNode);
    }

    private Collection<DbEntity> buildEntitiesInInsertOrder() {

        Set<DbEntity> dbEntities;

        if (entities.isEmpty() && entityGraphs.isEmpty()) {
            dbEntities = Collections.emptySet();
        } else if (entities.isEmpty()) {
            dbEntities = entityGraphs;
        } else if (entityGraphs.isEmpty()) {
            dbEntities = entities;
        } else {
            dbEntities = new HashSet<>();
            dbEntities.addAll(entities);
            dbEntities.addAll(entityGraphs);
        }

        // Sort if needed
        if (dbEntities.size() <= 1) {
            return dbEntities;
        }

        // Do not obtain sorter from Cayenne DI. It is not a singleton and will come uninitialized
        EntitySorter sorter = domain.getEntitySorter();
        List<DbEntity> sorted = new ArrayList<>(dbEntities);
        sorter.sortDbEntities(sorted, false);
        return sorted;
    }

    private void resolve(Set<DbEntity> accum, String tableName) {
        accum.add(dbEntityForName(tableName));
    }

    private void resolveGraph(Set<DbEntity> accum, String tableName) {
        DbEntity entity = dbEntityForName(tableName);
        ModelDependencyResolver.resolve(accum, entity);
    }

    private void resolve(Set<DbEntity> accum, Class<? extends Persistent> type) {
        accum.add(dbEntityForType(type));
    }

    private void resolveGraph(Set<DbEntity> accum, Class<? extends Persistent> type) {
        DbEntity entity = dbEntityForType(type);
        ModelDependencyResolver.resolve(accum, entity);
    }

    private void resolve(Set<DbEntity> accum, RelatedEntity re) {

        ObjEntity e = domain.getEntityResolver().getObjEntity(re.getType());
        if (e == null) {
            throw new IllegalStateException("Type is not mapped in Cayenne: " + re.getType());
        }

        ObjRelationship objRelationship = e.getRelationship(re.getRelationship());
        if (objRelationship == null) {
            throw new IllegalArgumentException("No relationship '" + re.getRelationship() + "' in entity " + e.getName());
        }

        List<DbRelationship> path = objRelationship.getDbRelationships();
        if (path.size() < 2) {
            return;
        }

        path.subList(1, path.size())
                .stream()
                .map(DbRelationship::getSourceEntity)
                .forEach(accum::add);
    }

    private DbEntity dbEntityForType(Class<? extends Persistent> type) {
        ObjEntity e = domain.getEntityResolver().getObjEntity(type);
        if (e == null) {
            throw new IllegalStateException("Type is not mapped in Cayenne: " + type);
        }

        return e.getDbEntity();
    }

    private DbEntity dbEntityForName(String name) {
        DbEntity dbe = domain.getEntityResolver().getDbEntity(name);
        if (dbe == null) {
            throw new IllegalStateException("Table is not mapped in Cayenne: " + name);
        }

        return dbe;
    }
}
