package io.bootique.cayenne.test;

import io.bootique.jdbc.test.Column;
import io.bootique.jdbc.test.DatabaseChannel;
import io.bootique.jdbc.test.Table;
import io.bootique.jdbc.test.TestDataManager;
import io.bootique.test.BQTestRuntime;
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

public class CayenneTestDataManager extends TestDataManager {

    private ServerRuntime runtime;

    public CayenneTestDataManager(BQTestRuntime runtime, Class<?>... entityTypes) {
        super(tablesInInsertOrder(runtime, entityTypes));
        this.runtime = runtime.getRuntime().getInstance(ServerRuntime.class);
    }

    private static Table[] tablesInInsertOrder(BQTestRuntime runtime, Class<?>... entityTypes) {

        ServerRuntime serverRuntime = runtime.getRuntime().getInstance(ServerRuntime.class);
        EntitySorter sorter = serverRuntime.getInjector().getInstance(EntitySorter.class);
        EntityResolver resolver = serverRuntime.getDataDomain().getEntityResolver();

        List<DbEntity> dbEntities = new ArrayList<>();

        for (Class<?> type : entityTypes) {
            dbEntities.add(getDbEntity(serverRuntime, type));
        }

        sorter.sortDbEntities(dbEntities, false);

        DatabaseChannel channel = DatabaseChannel.get(runtime);

        Table[] tables = new Table[dbEntities.size()];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = createTableModel(channel, dbEntities.get(0));
        }
        return tables;
    }

    public static Table createTableModel(BQTestRuntime runtime, Class<?> entityType) {
        ServerRuntime serverRuntime = runtime.getRuntime().getInstance(ServerRuntime.class);
        return createTableModel(DatabaseChannel.get(runtime), getDbEntity(serverRuntime, entityType));
    }

    public static Table createTableModel(BQTestRuntime runtime, String tableName) {
        ServerRuntime serverRuntime = runtime.getRuntime().getInstance(ServerRuntime.class);
        DbEntity dbEntity = serverRuntime.getDataDomain().getEntityResolver().getDbEntity(tableName);
        Objects.requireNonNull(dbEntity);
        return createTableModel(DatabaseChannel.get(runtime), dbEntity);
    }

    static DbEntity getDbEntity(ServerRuntime runtime, Class<?> entityType) {
        ObjEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(entityType);

        Objects.requireNonNull(entity);
        Objects.requireNonNull(entity.getDbEntityName());

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
                .build();
    }

    public Table getTable(Class<?> entityType) {
        return super.getTable(getDbEntity(runtime, entityType).getFullyQualifiedName());
    }
}
