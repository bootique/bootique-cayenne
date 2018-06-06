/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence.Table1;
import io.bootique.cayenne.test.persistence.Table2;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class CayenneTestDataManagerIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQRuntime TEST_RUNTIME;

    @Rule
    public CayenneTestDataManager dataManager = CayenneTestDataManager.builder(TEST_RUNTIME)
            .entities(Table1.class, Table2.class)
            .build();

    @BeforeClass
    public static void beforeClass() {
        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:config2.yml")
                .autoLoadModules()
                .createRuntime();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSuchTable() {
        dataManager.getTable(String.class);
    }

    @Test
    public void test1() {

        Table t1 = dataManager.getTable(Table1.class);
        Table t2 = dataManager.getTable(Table2.class);

        t1.matcher().assertNoMatches();
        t2.matcher().assertNoMatches();

        t1.insert(1, 2, 3);
        t2.insert(5, "x");

        t1.matcher().assertOneMatch();
        t2.matcher().assertOneMatch();
    }

    @Test
    public void test2() {

        Table t1 = dataManager.getTable(Table1.class);
        Table t2 = dataManager.getTable(Table2.class);

        t1.matcher().assertNoMatches();
        t2.matcher().assertNoMatches();

        t1.insert(4, 5, 6);
        t2.insert(7, "y");

        t1.matcher().assertOneMatch();
        t2.matcher().assertOneMatch();
    }
}
