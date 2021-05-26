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

package io.bootique.cayenne.v41.test;

import io.bootique.BQRuntime;
import io.bootique.jdbc.test.Column;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CayenneModelUtilsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testCreateTableModel() {
        BQRuntime runtime = testFactory.app("-c", "classpath:config1.yml")
                .autoLoadModules()
                .createRuntime();

        Table t1 = CayenneModelUtils.createTableModel(runtime, "db_entity");

        assertNotNull(t1);

        List<Column> columns = t1.getColumns();
        assertEquals(3, columns.size());
        assertEquals("a", columns.get(0).getName());
        assertEquals("b", columns.get(1).getName());
        assertEquals("id", columns.get(2).getName());
    }
}
