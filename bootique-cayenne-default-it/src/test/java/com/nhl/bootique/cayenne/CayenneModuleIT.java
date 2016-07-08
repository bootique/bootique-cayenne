package com.nhl.bootique.cayenne;

import com.nhl.bootique.jdbc.JdbcModule;
import com.nhl.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLSelect;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CayenneModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDefaultConfig() {

        ServerRuntime runtime = testFactory.newRuntime()
                .configurator(bootique -> bootique.modules(JdbcModule.class, CayenneModule.class))
                .build("--config=classpath:defaultconfig.yml")
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));

        // trigger DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity").select(runtime.newContext());
    }

    @Test
    public void testNoConfig_Builder() {

        // if "noConfig" is explicitly provided, we should be ignoring classpath:cayenne-project.xml

        ServerRuntime runtime = testFactory
                .newRuntime()
                .configurator(bootique -> bootique
                        .module(JdbcModule.class)
                        .module(CayenneModule.builder().noConfig().build()))
                .build("--config=classpath:defaultconfig.yml")
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertTrue(domain.getEntityResolver().getDbEntities().isEmpty());
    }
}
