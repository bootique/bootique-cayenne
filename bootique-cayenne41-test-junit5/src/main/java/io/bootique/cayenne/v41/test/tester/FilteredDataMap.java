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
package io.bootique.cayenne.v41.test.tester;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

import java.util.*;

/**
 * A DataMap decorator that provides access to a subset of DbEntities from another DataMap without changing their
 * parent. Unfortunately it is not easy tio just copy some entities from one DataMap to another, as the entities'
 * parent will get reset. Hence using thsi decorator.
 *
 * @since 2.0
 */
public class FilteredDataMap extends DataMap {

    // expected to be in the insert order
    private LinkedHashMap<String, DbEntity> orderedEntities;
    private List<DbEntity> entitiesInDeleteOrder;

    public FilteredDataMap(String mapName, LinkedHashMap<String, DbEntity> orderedEntities) {
        super(mapName);
        this.orderedEntities = orderedEntities;
    }

    public List<DbEntity> getEntitiesInDeleteOrder() {
        if (entitiesInDeleteOrder == null) {
            List<DbEntity> list = new ArrayList<>(orderedEntities.values());
            Collections.reverse(list);
            this.entitiesInDeleteOrder = list;
        }
        return entitiesInDeleteOrder;
    }

    public Collection<DbEntity> getDbEntitiesInInsertOrder() {
        return orderedEntities.values();
    }

    @Override
    public SortedMap<String, DbEntity> getDbEntityMap() {
        return new TreeMap<>(orderedEntities);
    }

    @Override
    public Collection<DbEntity> getDbEntities() {
        return getDbEntitiesInInsertOrder();
    }

    @Override
    public DbEntity getDbEntity(String dbEntityName) {
        return orderedEntities.get(dbEntityName);
    }
}
