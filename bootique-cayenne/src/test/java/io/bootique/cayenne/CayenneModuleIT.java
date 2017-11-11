package io.bootique.cayenne;

import com.google.inject.Module;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.DataDomainLoadException;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLSelect;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CayenneModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testFullConfig() {

        ServerRuntime runtime = testFactory.app("--config=classpath:fullconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void testConfig_ExplicitMaps_SharedDatasource() {

        ServerRuntime runtime = testFactory.app("--config=classpath:config_explicit_maps.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("map1", "SELECT * FROM db_entity").select(runtime.newContext());
        SQLSelect.dataRowQuery("map2", "SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void testConfig_ExplicitMaps_DifferentDatasources() {

        ServerRuntime runtime = testFactory.app("--config=classpath:config_explicit_maps_2.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("map1", "SELECT * FROM db_entity").select(runtime.newContext());
        SQLSelect.dataRowQuery("map2", "SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void testDefaultDataSource() throws SQLException {

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getDataNode("cayenne"));

        try(Connection c = domain.getDataNode("cayenne").getDataSource().getConnection();) {
            DatabaseMetaData md = c.getMetaData();
            assertEquals("jdbc:derby:target/derby/bqjdbc_noconfig", md.getURL());
        }
    }

    @Test
    public void testUndefinedDataSource() throws SQLException {

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig_2ds.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        try {
            runtime.getDataDomain();
        }
        catch (DataDomainLoadException e) {
            assertTrue(e.getCause().getMessage()
                    .startsWith("Can't map Cayenne DataSource: 'cayenne.datasource' is missing."));
        }
    }

    @Test
    public void testUnmatchedDataSource() throws SQLException {

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig_2ds_unmatched.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        try {
            runtime.getDataDomain();
            fail();
        }
        catch (DataDomainLoadException e) {
            String message = e.getCause().getMessage();
            assertEquals("No configuration present for DataSource named 'ds3'", message);
        }
    }

    @Test
    public void testNoConfig() {

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertTrue(domain.getEntityResolver().getDbEntities().isEmpty());
    }

    @Test
    public void testContributeModules() {

        Module guiceModule = b -> {
            org.apache.cayenne.di.Module cayenneModule = (cb) -> cb.bind(CayenneModuleIT.class).toInstance(this);
            CayenneModule.extend(b).addModule(cayenneModule);
        };

        ServerRuntime runtime = testFactory.app("--config=classpath:fullconfig.yml")
                .autoLoadModules()
                .module(guiceModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);

        assertSame(this, runtime.getInjector().getInstance(CayenneModuleIT.class));
    }

    @Test
    public void testMergeConfigs() {

        Module cayenneProjectModule = binder -> CayenneModule.extend(binder).addProject("cayenne-project2.xml");

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig.yml")
                .autoLoadModules()
                .module(cayenneProjectModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertFalse(domain.getEntityResolver().getDbEntities().isEmpty());
    }
}
