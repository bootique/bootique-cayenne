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
import io.bootique.Bootique;
import io.bootique.cayenne.v41.test.persistence.Table1;
import io.bootique.cayenne.v41.test.persistence.Table2;
import io.bootique.jdbc.test.DbTester;
import io.bootique.jdbc.test.Table;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.apache.cayenne.Persistent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@BQTest
public class CayenneTesterIT {

    @RegisterExtension
    static final DbTester db = DbTester.derbyDb();

    @RegisterExtension
    static final CayenneTester cayenne = CayenneTester
            .create()
            .entities(Table1.class, Table2.class)
            .deleteBeforeEachTest();

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique
            .app("-c", "classpath:config2.yml")
            .autoLoadModules()
            .module(db.setOrReplaceDataSource("db"))
            .module(cayenne.registerTestHooks())
            .createRuntime();

    @Test
    public void testNoSuchTable() {
        assertThrows(IllegalStateException.class, () -> cayenne.getTableName(Persistent.class));
    }

    @Test
    public void test1() {

        Table t1 = db.getTable(cayenne.getTableName(Table1.class));
        Table t2 = db.getTable(cayenne.getTableName(Table2.class));

        t1.matcher().assertNoMatches();
        t2.matcher().assertNoMatches();

        t1.insert(1, 2, 3);
        t2.insert(5, "x");

        t1.matcher().assertOneMatch();
        t2.matcher().assertOneMatch();
    }

    @Test
    public void test2() {

        Table t1 = db.getTable(cayenne.getTableName(Table1.class));
        Table t2 = db.getTable(cayenne.getTableName(Table2.class));

        t1.matcher().assertNoMatches();
        t2.matcher().assertNoMatches();

        t1.insert(4, 5, 6);
        t2.insert(7, "y");

        t1.matcher().assertOneMatch();
        t2.matcher().assertOneMatch();
    }
}
