package io.bootique.cayenne.test;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
