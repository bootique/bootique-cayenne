package io.bootique.cayenne.test;

import io.bootique.jdbc.test.Column;
import io.bootique.jdbc.test.DatabaseChannel;
import io.bootique.jdbc.test.Table;
import io.bootique.test.BQTestRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

import java.util.Arrays;
import java.util.Objects;

/**
 * A wrapper for {@link DatabaseChannel} that allows to create {@link Table} instances out of Cayenne model objects.
 *
 * @since 0.18
 */
public class CayenneTestUtil {

    public static Table getTable(BQTestRuntime runtime, Class<?> entityType) {

        ServerRuntime serverRuntime = runtime.getRuntime().getInstance(ServerRuntime.class);
        ObjEntity entity = serverRuntime.getDataDomain().getEntityResolver().getObjEntity(entityType);
        Objects.requireNonNull(entity);
        return getTable(runtime, entity.getDbEntity());
    }

    public static Table getTable(BQTestRuntime runtime, String tableName) {
        ServerRuntime serverRuntime = runtime.getRuntime().getInstance(ServerRuntime.class);
        DbEntity dbEntity = serverRuntime.getDataDomain().getEntityResolver().getDbEntity(tableName);
        Objects.requireNonNull(dbEntity);
        return getTable(runtime, dbEntity);
    }

    static Table getTable(BQTestRuntime runtime, DbEntity dbEntity) {

        DatabaseChannel channel = DatabaseChannel.get(runtime);

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
}
