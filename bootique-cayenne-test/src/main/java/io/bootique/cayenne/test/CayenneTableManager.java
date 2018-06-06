/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.cayenne.test;

import io.bootique.jdbc.test.DatabaseChannel;
import io.bootique.jdbc.test.Table;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a map of {@link io.bootique.jdbc.test.Table} objects matching each Cayenne DbEntity.
 *
 * @since 0.18
 */
public class CayenneTableManager {

    private DatabaseChannel channel;
    private EntityResolver resolver;
    private Map<String, Table> tables;

    public CayenneTableManager(EntityResolver resolver, DatabaseChannel channel) {
        this.resolver = resolver;
        this.channel = channel;
        this.tables = new ConcurrentHashMap<>();
    }

    /**
     * Returns a Table related to a given entity via the specified relationship. Useful for navigation to join tables
     * that are not directly mapped to Java classes.
     *
     * @param entityType   a root entity used to resolve a join table.
     * @param relationship a Property indicating an ObjRelationship.
     * @param tableIndex   An index in a list of tables spanned by 'relationship'. Index of 0 corresponds to the target
     *                     DbEntity of the first object in a chain of DbRelationships for a given ObjRelationship.
     * @return a Table related to a given entity via the specified relationship.
     * @since 0.24
     */
    public Table getRelatedTable(Class<?> entityType, Property<?> relationship, int tableIndex) {
        ObjEntity entity = resolver.getObjEntity(entityType);
        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne entity class: " + entityType.getName());
        }

        ObjRelationship flattened = entity.getRelationship(relationship.getName());

        if (flattened == null) {
            throw new IllegalArgumentException("No relationship '" + relationship.getName() + "' in entity " + entityType.getName());
        }

        List<DbRelationship> path = flattened.getDbRelationships();

        if (path.size() < tableIndex + 1) {
            throw new IllegalArgumentException("Index " + tableIndex + " is out of bounds for relationship '" + relationship.getName());
        }

        return getTable(path.get(tableIndex).getTargetEntityName());
    }

    public Table getTable(Class<?> entityType) {
        ObjEntity entity = resolver.getObjEntity(entityType);
        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne entity class: " + entityType.getName());
        }

        if (entity.getDbEntityName() == null) {
            throw new IllegalArgumentException("Cayenne entity class is not mapped to a DbEntity: " + entityType.getName());
        }

        return getTable(entity.getDbEntityName());
    }

    public Table getTable(String name) {

        return tables.computeIfAbsent(name, n -> {
            DbEntity entity = resolver.getDbEntity(n);
            if (entity == null) {
                throw new IllegalArgumentException("Unknown DbEntity: " + n);
            }

            return CayenneModelUtils.createTableModel(channel, entity);
        });
    }
}
