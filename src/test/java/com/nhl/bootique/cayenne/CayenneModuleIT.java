package com.nhl.bootique.cayenne;

import com.nhl.bootique.jdbc.JdbcModule;
import com.nhl.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CayenneModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDefaultConfig() {

        ServerRuntime runtime = testFactory.newRuntime().configurator(bootique -> bootique.modules(JdbcModule.class,
                CayenneModule.class)).build().getRuntime().getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
    }

    @Test
    public void testNoConfig() {

        ServerRuntime runtime = testFactory
                .newRuntime()
                .configurator(bootique -> bootique
                        .module(JdbcModule.class)
                        .module(CayenneModule.builder().noConfig().build()))
                .build()
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertTrue(domain.getEntityResolver().getDbEntities().isEmpty());
    }
}
