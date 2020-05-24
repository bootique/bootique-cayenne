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
import io.bootique.cayenne.v41.junit5.CayenneTester;
import io.bootique.cayenne.v41.junit5.persistence.Table1;
import io.bootique.cayenne.v41.junit5.persistence.Table2;
import io.bootique.jdbc.junit5.DbTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.apache.cayenne.ObjectContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

@BQTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CayenneTesterCachesNotRefreshedIT {

    @RegisterExtension
    static final DbTester db = DbTester.derbyDb();

    @RegisterExtension
    static final CayenneTester cayenne = CayenneTester
            .create()
            .doNoRefreshCayenneCaches()
            .entities(Table1.class, Table2.class);

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app("-c", "classpath:config2.yml")
            .autoLoadModules()
            .module(db.setOrReplaceDataSource("db"))
            .module(cayenne.registerTestHooks())
            .createRuntime();

    @Test
    @Order(1)
    public void crossTestInterference1() {
        verifyCachesEmptyAndAddObjectsToCache();
    }

    @Test
    @Order(2)
    public void crossTestInterference2() {
        verifyCaches(1);
    }

    private void verifyCaches(int expectedCount) {
        Assertions.assertEquals(expectedCount, cayenne.getRuntime().getDataDomain().getSharedSnapshotCache().size());
    }

    private void verifyCachesEmptyAndAddObjectsToCache() {
        // verify that there's no data in the cache
        verifyCaches(0);

        // seed the cache for the next test
        ObjectContext context = cayenne.getRuntime().newContext();
        Table1 t1 = context.newObject(Table1.class);
        t1.setA(5L);
        t1.setB(6L);
        context.commitChanges();
    }
}
