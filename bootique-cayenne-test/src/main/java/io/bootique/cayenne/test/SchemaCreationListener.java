package io.bootique.cayenne.test;

import io.bootique.jdbc.test.runtime.DataSourceListener;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.configuration.server.ServerRuntime;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Would create DB schema for DataSources matching those of a Cayenne runtime.
 *
 * @since 0.17
 */
public class SchemaCreationListener implements DataSourceListener {

    private List<Consumer<Collection<DataNode>>> schemaGenerators = new ArrayList<>();

    /**
     * Invoked explicitly after all {@link DataSourceListener} methods are processed to generate DB schemas based on
     * Cayenne stack.
     *
     * @param runtime Cayenne stack for which we need to complete schema generation.
     */
    public void createSchemas(ServerRuntime runtime) {

        // this line may trigger DataSource resolution for each node, inflating 'schemaGenerators' collection.
        // (i.e. using Consumer<ServerRuntime> instead of Consumer<Collection<DataNode>> will not work,
        // as 'schemaGenerators' will be empty
        Collection<DataNode> nodes = runtime.getDataDomain().getDataNodes();

        // run in parallel? I guess most tests will have just one schema, so this would be moot...
        schemaGenerators.forEach(g -> g.accept(nodes));
    }

    @Override
    public void beforeStartup(String name, Optional<String> jdbcUrl) {
        // ignore
    }

    @Override
    public void afterShutdown(String name, Optional<String> jdbcUrl) {
        // ignore
    }

    @Override
    public void afterStartup(String name, Optional<String> jdbcUrl, DataSource dataSource) {

        // defer schema creation until Cayenne is fully loaded...
        schemaGenerators.add(nodes -> matchingDataNodes(nodes, dataSource).forEach(this::createSchema));
    }

    protected Stream<DataNode> matchingDataNodes(Collection<DataNode> nodes, DataSource dataSource) {
        return nodes.stream().filter(n -> {

            // fuzzy logic for finding a DataNode with a given DataSource...
            // relying on the fact that Cayenne DataSource is a one-level wrapper around
            // factory-provided DS..
            // TODO: May fail in the future, so perhaps we need to match by JDBC URL?

            try {
                return n.getDataSource().unwrap(DataSource.class) == dataSource;
            } catch (SQLException e) {
                throw new RuntimeException("Error unwrapping DataSource", e);
            }
        });
    }

    protected void createSchema(DataNode node) {

        // not using Cayenne CreateIfNoSchemaStrategy, as we do not need to check table presence. Rather copy/pasting
        // relevant code from it....

        node.getDataMaps().forEach(map -> {

            DbGenerator generator = new DbGenerator(node.getAdapter(), map, node.getJdbcEventLogger());
            generator.setShouldCreateTables(true);
            generator.setShouldDropTables(false);
            generator.setShouldCreateFKConstraints(true);
            generator.setShouldCreatePKSupport(true);
            generator.setShouldDropPKSupport(false);

            try {
                generator.runGenerator(node.getDataSource());
            } catch (Exception e) {
                throw new RuntimeException("Error creating schema for DataNode: " + node.getName(), e);
            }
        });
    }
}
