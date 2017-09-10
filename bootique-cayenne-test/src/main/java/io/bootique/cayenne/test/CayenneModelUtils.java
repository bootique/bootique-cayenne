package io.bootique.cayenne.test;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    static Table[] tablesInInsertOrder(BQRuntime runtime, List<DbEntity> dbEntities) {

        // note: do not obtain sorter from Cayenne DI. It is not a singleton and will come
        // uninitialized
        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        EntitySorter sorter = serverRuntime.getDataDomain().getEntitySorter();

        sorter.sortDbEntities(dbEntities, false);

        DatabaseChannel channel = DatabaseChannel.get(runtime);

        Table[] tables = new Table[dbEntities.size()];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = createTableModel(channel, dbEntities.get(i));
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
