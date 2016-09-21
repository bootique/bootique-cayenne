package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.jdbc.test.DatabaseChannel;
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.configuration.server.ServerRuntime;

import java.sql.SQLException;
import java.util.function.BiConsumer;

/**
 * @since 0.17
 */
public class SchemaCreator implements BiConsumer<BQRuntime, DatabaseChannel> {

    @Override
    public void accept(BQRuntime bqRuntime, DatabaseChannel databaseChannel) {

        SchemaUpdateStrategy schemaCreator = new CreateIfNoSchemaStrategy();
        bqRuntime.getInstance(ServerRuntime.class).getDataDomain().getDataNodes().forEach(dn -> {
            try {
                schemaCreator.updateSchema(dn);
            } catch (SQLException e) {
                throw new RuntimeException("Error creating schema for DataNode: " + dn.getName(), e);
            }
        });
    }
}
