package io.bootique.cayenne;

import io.bootique.jdbc.JdbcModule;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLSelect;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CayenneModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDefaultConfig() {

        ServerRuntime runtime = testFactory.app("--config=classpath:defaultconfig.yml")
                .modules(JdbcModule.class, CayenneModule.class)
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));

        // trigger DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity").select(runtime.newContext());
    }
}
