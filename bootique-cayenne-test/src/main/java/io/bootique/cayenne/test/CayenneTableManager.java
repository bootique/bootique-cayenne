package io.bootique.cayenne.test;

import io.bootique.jdbc.test.DatabaseChannel;
import io.bootique.jdbc.test.Table;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

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

            return CayenneTestDataManager.createTableModel(channel, entity);
        });

    }
}
