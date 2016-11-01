package io.bootique.cayenne;

import com.google.inject.Module;
import io.bootique.jdbc.JdbcModule;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLSelect;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CayenneModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testFullConfig() {

        ServerRuntime runtime = testFactory.app("--config=classpath:fullconfig.yml")
                .modules(JdbcModule.class, CayenneModule.class)
                .createRuntime()
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void testConfig_ExplicitMaps_SharedDatasource() {

        ServerRuntime runtime = testFactory.app("--config=classpath:config_explicit_maps.yml")
                .modules(JdbcModule.class, CayenneModule.class)
                .createRuntime()
                .getRuntime()
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
                .modules(JdbcModule.class, CayenneModule.class)
                .createRuntime()
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("map1", "SELECT * FROM db_entity").select(runtime.newContext());
        SQLSelect.dataRowQuery("map2", "SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void testNoConfig() {

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig.yml")
                .modules(JdbcModule.class, CayenneModule.class)
                .createRuntime()
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertTrue(domain.getEntityResolver().getDbEntities().isEmpty());
    }

    @Test
    public void testContributeModules() {

        Module guiceModule = b -> {
            org.apache.cayenne.di.Module cayenneModule = (cb) -> {
                cb.bind(CayenneModuleIT.class).toInstance(this);
            };
            CayenneModule.contributeModules(b).addBinding().toInstance(cayenneModule);
        };

        ServerRuntime runtime = testFactory.app("--config=classpath:fullconfig.yml")
                .modules(JdbcModule.class, CayenneModule.class).module(guiceModule)
                .createRuntime()
                .getRuntime()
                .getInstance(ServerRuntime.class);

        assertSame(this, runtime.getInjector().getInstance(CayenneModuleIT.class));
    }

    @Test
    public void testMergeConfigs() {

        Module cayenneProjectModule = binder -> CayenneModule.contributeProjects(binder)
                .addBinding().toInstance("cayenne-project2.xml");

        ServerRuntime runtime = testFactory.app("--config=classpath:noconfig.yml")
                .modules(JdbcModule.class, CayenneModule.class)
                .module(cayenneProjectModule)
                .createRuntime()
                .getRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertFalse(domain.getEntityResolver().getDbEntities().isEmpty());
    }
}
