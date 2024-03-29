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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @deprecated phasing out JUnit 4 support in favor of JUnit 5, same for Cayenne 4.1 in favor of 4.2
 */
@Deprecated(since = "3.0", forRemoval = true)
class ModelDependencyResolver {

    static Set<DbEntity> resolve(Collection<DbEntity> entities) {
        Set<DbEntity> resolved = new HashSet<>();
        entities.forEach(e -> resolve(resolved, e));
        return resolved;
    }

    static private void resolve(Set<DbEntity> resolved, DbEntity entity) {

        if (resolved.add(entity)) {
            entity.getRelationships().forEach(r -> resolveDependent(resolved, r));
        }
    }

    static private void resolveDependent(Set<DbEntity> resolved, DbRelationship relationship) {
        if (relationship.isFromPK() && !relationship.isToMasterPK()) {
            resolve(resolved, relationship.getTargetEntity());
        }
    }
}
