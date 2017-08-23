package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.jdbc.test.Column;
import io.bootique.jdbc.test.DatabaseChannel;
import io.bootique.jdbc.test.Table;
import io.bootique.jdbc.test.TestDataManager;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @since 0.18
 */
public class CayenneTestDataManager extends TestDataManager {

    private CayenneTableManager tableManager;

    /**
     * Creates a builder of CayenneTestDataManager
     *
     * @param runtime {@link BQRuntime} used in the test.
     * @return a new instance of CayenneTestDataManager builder.
     * @since 0.24
     */
    public static Builder builder(BQRuntime runtime) {
        return new Builder(runtime);
    }

    /**
     * @param runtime
     * @param deleteData
     * @param entityTypes
     * @deprecated since 0.24 use {@link #builder(BQRuntime)} to create the data manager.
     */
    @Deprecated
    public CayenneTestDataManager(BQRuntime runtime, boolean deleteData, Class<?>... entityTypes) {
        super(deleteData, tablesInInsertOrder(runtime, entityTypes));
        this.tableManager = runtime.getInstance(CayenneTableManager.class);
    }

    /**
     * @param deleteData          whether all managed tables should be deleted before each test.
     * @param tableManager        an object that maps Cayenne DbEntities to Tables.
     * @param tablesInInsertOrder a subset of of all tables managed by this object.
     * @since 0.24
     */
    protected CayenneTestDataManager(boolean deleteData, CayenneTableManager tableManager, Table... tablesInInsertOrder) {
        super(deleteData, tablesInInsertOrder);
        this.tableManager = tableManager;
    }

    private static Table[] tablesInInsertOrder(BQRuntime runtime, Class<?>... entityTypes) {

        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        EntityResolver resolver = serverRuntime.getDataDomain().getEntityResolver();

        List<DbEntity> dbEntities = new ArrayList<>();

        for (Class<?> type : entityTypes) {
            dbEntities.add(getDbEntity(resolver, type));
        }

        return tablesInInsertOrder(runtime, dbEntities);
    }

    private static Table[] tablesInInsertOrder(BQRuntime runtime, List<DbEntity> dbEntities) {

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

    public static Table createTableModel(BQRuntime runtime, Class<?> entityType) {
        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        return createTableModel(DatabaseChannel.get(runtime), getDbEntity(
                serverRuntime.getDataDomain().getEntityResolver(),
                entityType));
    }

    public static Table createTableModel(BQRuntime runtime, String tableName) {
        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        DbEntity dbEntity = serverRuntime.getDataDomain().getEntityResolver().getDbEntity(tableName);
        Objects.requireNonNull(dbEntity);
        return createTableModel(DatabaseChannel.get(runtime), dbEntity);
    }

    private static DbEntity getDbEntity(EntityResolver resolver, Class<?> entityType) {
        ObjEntity entity = resolver.getObjEntity(entityType);

        if (entity == null) {
            throw new IllegalArgumentException("Not a Cayenne entity class: " + entityType.getName());
        }

        if (entity.getDbEntityName() == null) {
            throw new IllegalArgumentException("Cayenne entity class is not mapped to a DbEntity: " + entityType.getName());
        }

        return entity.getDbEntity();
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

    public Table getTable(Class<?> entityType) {
        return tableManager.getTable(entityType);
    }

    public static class Builder {
        private BQRuntime runtime;
        private boolean deleteData;
        private List<DbEntity> entities;
        private EntityResolver resolver;

        private Builder(BQRuntime runtime) {
            this.runtime = runtime;

            ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
            this.resolver = serverRuntime.getDataDomain().getEntityResolver();
            this.entities = new ArrayList<>();
        }

        public CayenneTestDataManager build() {
            return new CayenneTestDataManager(deleteData,
                    runtime.getInstance(CayenneTableManager.class),
                    tablesInInsertOrder(runtime, entities));
        }

        public Builder deleteData() {
            this.deleteData = true;
            return this;
        }

        public Builder entities(Class<?>... entityTypes) {

            if (entityTypes != null) {
                for (Class<?> type : entityTypes) {
                    entities.add(getDbEntity(resolver, type));
                }
            }

            return this;
        }
    }
}
