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

package io.bootique.cayenne.v50.junit5;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.cayenne.v50.junit5.persistence.Table1;
import io.bootique.cayenne.v50.junit5.persistence.Table2;
import io.bootique.jdbc.junit5.derby.DerbyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.RepeatedTest;

@BQTest
public class CayenneTester_AssertOpCountsIT {

    @BQTestTool
    static final DerbyTester db = DerbyTester.db();

    @BQTestTool
    static final CayenneTester cayenne = CayenneTester
            .create()
            .entities(Table1.class, Table2.class)
            .deleteBeforeEachTest();

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique
            .app("-c", "classpath:config2.yml")
            .autoLoadModules()
            .module(db.moduleWithTestDataSource("db"))
            .module(cayenne.moduleWithTestHooks())
            .createRuntime();

    @RepeatedTest(3)
    public void commitCount() {

        // must be reset at every run
        cayenne.assertCommitCount(0);
        ObjectContext context = cayenne.getRuntime().newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setA(7L);
        t1.setB(8L);
        context.commitChanges();

        cayenne.assertCommitCount(1);
    }

    @RepeatedTest(3)
    public void queryCount() {

        // must be reset at every run
        cayenne.assertQueryCount(0);
        ObjectContext context = cayenne.getRuntime().newContext();

        ObjectSelect.query(Table1.class).select(context);
        cayenne.assertQueryCount(1);

        ObjectSelect.query(Table1.class).select(context);
        cayenne.assertQueryCount(2);

        // reset context, same counter should be in use
        ObjectSelect.query(Table1.class).select(cayenne.getRuntime().newContext());
        cayenne.assertQueryCount(3);
    }
}
