/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.cayenne.v42;

import io.bootique.BQModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.DataDomainLoadException;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.query.SQLSelect;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class CayenneModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void noConfig() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertEquals("cayenne", domain.getName());
        assertEquals("ds", domain.getDefaultNode().getName());

        assertTrue(domain.getEntityResolver().getDbEntities().isEmpty());
    }

    @Test
    public void noConfig_Name() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig-named.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertEquals("my", domain.getName());
        assertEquals("ds", domain.getDefaultNode().getName());
    }

    @Test
    public void fullConfig() {
        ServerRuntime runtime = testFactory.app("-c", "classpath:fullconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void addLocation_2Locations() {

        BQModule cayenneProjectModule = binder -> CayenneModule.extend(binder)
                .addLocation("classpath:cayenne-project2.xml")
                .addLocation("classpath:cayenne-project1.xml");

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig.yml")
                .autoLoadModules()
                .module(cayenneProjectModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void twoConfigs() {
        ServerRuntime runtime = testFactory.app("-c", "classpath:twoconfigs.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void config_ExplicitMaps_SharedDataSource() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:config_explicit_maps.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("datamap1", "SELECT * FROM db_entity").select(runtime.newContext());
        SQLSelect.dataRowQuery("datamap2", "SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void config_ExplicitMaps_DifferentDataSources() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:config_explicit_maps_2.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("datamap1", "SELECT * FROM db_entity").select(runtime.newContext());
        SQLSelect.dataRowQuery("datamap2", "SELECT * FROM db_entity2").select(runtime.newContext());
    }

    @Test
    public void defaultDataSource() throws SQLException {

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getDataNode("ds"));

        try (Connection c = domain.getDataNode("ds").getDataSource().getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            assertEquals("jdbc:derby:target/derby/bqjdbc_noconfig", md.getURL());
        }
    }

    @Test
    public void undefinedDataSource() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig_2ds.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        try {
            runtime.getDataDomain();
        } catch (DataDomainLoadException e) {
            assertTrue(e.getCause().getMessage()
                    .startsWith("Can't map Cayenne DataSource: 'cayenne.datasource' is missing."));
        }
    }

    @Test
    public void unmatchedDataSource() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig_2ds_unmatched.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        try {
            runtime.getDataDomain();
            fail();
        } catch (DataDomainLoadException e) {
            String message = e.getCause().getMessage();
            assertEquals("No configuration present for DataSource named 'ds3'", message);
        }
    }

    @Test
    public void contributeModules() {

        Key<Object> key = Key.get(Object.class, "_test_");
        Object value = new Object();

        BQModule bqModule = b -> {
            org.apache.cayenne.di.Module cayenneModule = (cb) -> cb.bind(key).toInstance(value);
            CayenneModule.extend(b).addModule(cayenneModule);
        };

        ServerRuntime runtime = testFactory.app("-c", "classpath:fullconfig.yml")
                .autoLoadModules()
                .module(bqModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);

        assertSame(value, runtime.getInjector().getInstance(key));
    }

    @Test
    public void mergeConfigs() {

        BQModule cayenneProjectModule = binder -> CayenneModule.extend(binder).addProject("cayenne-project2.xml");

        ServerRuntime runtime = testFactory.app("-c", "classpath:noconfig.yml")
                .autoLoadModules()
                .module(cayenneProjectModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertFalse(domain.getEntityResolver().getDbEntities().isEmpty());
    }

    @Test
    public void configMaps_Plus_AddProject_DataSourceAssignment() {

        // see https://github.com/bootique/bootique-cayenne/issues/69
        // module-provided project has no DataNode .. must be assigned the default one

        ServerRuntime runtime = testFactory
                .app("--config=classpath:ConfigMaps_Plus_AddProject_DataSourceAssignment.yml")
                .autoLoadModules()
                .module(b -> CayenneModule.extend(b).addLocation("classpath:cayenne-project1.xml"))
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();

        assertEquals(3, domain.getDataMaps().size());
        assertNotNull(domain.getDataMap("datamap1"));
        assertNotNull(domain.getDataMap("datamap2"));
        assertNotNull(domain.getDataMap("datamap3"));

        assertEquals(2, domain.getDataNodes().size());
        assertNotNull(domain.getDataNode("ds1"));
        assertNotNull(domain.getDataNode("ds2"));

        assertSame(domain.getDataNode("ds1"), domain.lookupDataNode(domain.getDataMap("datamap1")));
        assertSame(domain.getDataNode("ds2"), domain.lookupDataNode(domain.getDataMap("datamap2")));
        assertSame(domain.getDataNode("ds1"), domain.lookupDataNode(domain.getDataMap("datamap3")));
    }

    @Test
    public void config_ExplicitMaps_DifferentConfig() {

        ServerRuntime runtime = testFactory.app("-c", "classpath:config_mapDatasources.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        DataDomain domain = runtime.getDataDomain();
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
        assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

        // trigger a DB op
        SQLSelect.dataRowQuery("datamap1", "SELECT * FROM db_entity").select(runtime.newContext());
        SQLSelect.dataRowQuery("datamap2", "SELECT * FROM db_entity2").select(runtime.newContext());
    }
}
