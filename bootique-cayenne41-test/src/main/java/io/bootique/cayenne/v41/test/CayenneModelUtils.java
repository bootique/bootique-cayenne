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

package io.bootique.cayenne.v41.test;

import io.bootique.BQRuntime;
import io.bootique.jdbc.test.Column;
import io.bootique.jdbc.test.DatabaseChannel;
import io.bootique.jdbc.test.Table;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.*;
import java.util.stream.Stream;

/**
 * @deprecated phasing out JUnit 4 support in favor of JUnit 5, same for Cayenne 4.1 in favor of 4.2
 */
@Deprecated(since = "3.0", forRemoval = true)
class CayenneModelUtils {

    static Table createTableModel(BQRuntime runtime, Class<?> entityType) {
        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        return createTableModel(DatabaseChannel.get(runtime), getDbEntity(
                serverRuntime.getDataDomain().getEntityResolver(),
                entityType));
    }

    static Table createTableModel(BQRuntime runtime, String tableName) {
        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        DbEntity dbEntity = serverRuntime.getDataDomain().getEntityResolver().getDbEntity(tableName);
        Objects.requireNonNull(dbEntity);
        return createTableModel(DatabaseChannel.get(runtime), dbEntity);
    }

    static Table createTableModel(DatabaseChannel channel, DbEntity dbEntity) {

        Column[] columns = new Column[dbEntity.getAttributes().size()];
        int i = 0;
        for (DbAttribute a : dbEntity.getAttributes()) {
            columns[i++] = new Column(Objects.requireNonNull(a.getName()), a.getType());
        }

        // ensure predictable column order .. DbEntity's attributes are presumably already sorted, but just in case
        Arrays.sort(columns, (c1, c2) -> c1.getName().compareTo(c2.getName()));

        return Table.builder(channel, dbEntity.getFullyQualifiedName())
                .columns(columns)
                .quoteSqlIdentifiers(dbEntity.getDataMap().isQuotingSQLIdentifiers())
                .build();
    }

    static Table[] tablesInInsertOrder(BQRuntime runtime, Class<?>... entityTypes) {

        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        EntityResolver resolver = serverRuntime.getDataDomain().getEntityResolver();

        List<DbEntity> dbEntities = new ArrayList<>();

        for (Class<?> type : entityTypes) {
            dbEntities.add(getDbEntity(resolver, type));
        }

        return tablesInInsertOrder(runtime, dbEntities);
    }

    static Table[] tablesInInsertOrder(BQRuntime runtime, Collection<DbEntity> dbEntities) {

        // note: do not obtain sorter from Cayenne DI. It is not a singleton and will come
        // uninitialized
        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        EntitySorter sorter = serverRuntime.getDataDomain().getEntitySorter();

        List<DbEntity> list = new ArrayList<>(dbEntities);
        sorter.sortDbEntities(list, false);

        DatabaseChannel channel = DatabaseChannel.get(runtime);

        Table[] tables = new Table[list.size()];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = createTableModel(channel, list.get(i));
        }
        return tables;
    }

    static DbEntity getDbEntity(EntityResolver resolver, Class<?> entityType) {
        ObjEntity entity = resolver.getObjEntity(entityType);

        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne entity class: " + entityType.getName());
        }

        if (entity.getDbEntityName() == null) {
            throw new IllegalArgumentException("Cayenne entity class is not mapped to a DbEntity: " + entityType.getName());
        }

        return entity.getDbEntity();
    }

    static DbEntity getDbEntity(EntityResolver resolver, String tableName) {
        DbEntity entity = resolver.getDbEntity(tableName);
        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne-managed table: " + tableName);
        }

        return entity;
    }

    static Stream<DbEntity> getRelatedDbEntities(EntityResolver resolver, Class<?> entityType, Property<?> relationship) {
        ObjEntity entity = resolver.getObjEntity(entityType);

        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne entity class: " + entityType.getName());
        }

        ObjRelationship objRelationship = entity.getRelationship(relationship.getName());

        if (objRelationship == null) {
            throw new IllegalArgumentException("No relationship '" + relationship.getName() + "' in entity " + entityType.getName());
        }

        List<DbRelationship> path = objRelationship.getDbRelationships();
        if (path.size() < 2) {
            return Stream.empty();
        }

        return path.subList(1, path.size()).stream().map(DbRelationship::getSourceEntity);
    }
}
