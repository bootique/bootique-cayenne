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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

import java.util.Collection;
import java.util.Map;

/**
 * A DataMap decorator that provides access to a subset of DbEntities from another DataMap without changing their
 * parent.
 *
 * @since 2.0
 */
public class FilteredDataMap extends DataMap {

    private Map<String, DbEntity> includedEntities;

    public FilteredDataMap(String mapName, Map<String, DbEntity> includedEntities) {
        super(mapName);
        this.includedEntities = includedEntities;
    }

    @Override
    public Collection<DbEntity> getDbEntities() {
        return includedEntities.values();
    }

    @Override
    public DbEntity getDbEntity(String dbEntityName) {
        return includedEntities.get(dbEntityName);
    }
}
