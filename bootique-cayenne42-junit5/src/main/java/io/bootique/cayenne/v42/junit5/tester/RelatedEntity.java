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
package io.bootique.cayenne.v42.junit5.tester;

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

    public DbEntity getTarget(EntityResolver resolver) {
        ObjEntity e = resolver.getObjEntity(type);
        if (e == null) {
            throw new IllegalStateException("Type is not mapped in Cayenne: " + type);
        }

        ObjRelationship objRelationship = e.getRelationship(relationship);
        if (objRelationship == null) {
            throw new IllegalArgumentException("No relationship '" + relationship + "' in entity " + e.getName());
        }

        List<DbRelationship> path = objRelationship.getDbRelationships();
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Unmapped relationship '" + relationship + "' in entity " + e.getName());
        }

        return path.get(path.size() - 1).getTargetEntity();
    }
}
