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

package io.bootique.cayenne.v41.junit5;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.cayenne.v41.junit5.tester.FilteredDataMap;
import io.bootique.jdbc.junit5.derby.DerbyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DbEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class CayenneTester_AllTablesIT {

    @BQTestTool
    static final DerbyTester db = DerbyTester.db();

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique
            .app("-c", "classpath:config2.yml")
            .autoLoadModules()
            .module(db.moduleWithTestDataSource("db"))
            .createRuntime();

    private Set<String> getTables(CayenneTester ct) {
        ct.createRuntimeManager(app.getInstance(ServerRuntime.class));
        Map<String, FilteredDataMap> entities = ct.getRuntimeManager().getManagedEntitiesByNode();

        assertEquals(1, entities.size());
        FilteredDataMap map = entities.values().iterator().next();

        return map.getDbEntitiesInInsertOrder()
                .stream()
                .map(DbEntity::getName)
                .collect(Collectors.toSet());
    }

    @Test
    public void testAllTables() {

        CayenneTester ct = CayenneTester.create().allTables();
        Set<String> tables = getTables(ct);

        assertEquals(2, tables.size());
        assertTrue(tables.contains("table1"));
        assertTrue(tables.contains("table2"));
    }
}
