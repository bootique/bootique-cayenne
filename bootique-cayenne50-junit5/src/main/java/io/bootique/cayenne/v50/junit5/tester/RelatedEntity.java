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
package io.bootique.cayenne.v50.junit5.tester;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.*;

import java.util.List;

/**
 * @since 2.0
 */
public class RelatedEntity {

    private Class<? extends Persistent> type;
    private String relationship;

    public RelatedEntity(Class<? extends Persistent> type, String relationship) {
        this.type = type;
        this.relationship = relationship;
    }

    public Class<? extends Persistent> getType() {
        return type;
    }

    public String getRelationship() {
        return relationship;
    }

    /**
     * Returns a DbEntity related to a given entity via the specified relationship. Useful for navigation to join tables
     * that are not directly mapped to Java classes.
     *
     * @param tableIndex   An index in a list of tables spanned by 'relationship'. Index of 0 corresponds to the target
     *                     DbEntity of the first object in a chain of DbRelationships for a given ObjRelationship.
     * @return a DbEntity related to a given entity via the specified relationship.
     */
    public DbEntity getRelatedTable(EntityResolver resolver, int tableIndex) {
        ObjEntity entity = resolver.getObjEntity(type);
        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne entity class: " + type.getName());
        }

        ObjRelationship flattened = entity.getRelationship(relationship);

        if (flattened == null) {
            throw new IllegalArgumentException("No relationship '" + relationship + "' in entity " + type.getName());
        }

        List<DbRelationship> path = flattened.getDbRelationships();

        if (path.size() < tableIndex + 1) {
            throw new IllegalArgumentException("Index " + tableIndex + " is out of bounds for relationship '" + relationship);
        }

        return path.get(tableIndex).getTargetEntity();
    }
}
