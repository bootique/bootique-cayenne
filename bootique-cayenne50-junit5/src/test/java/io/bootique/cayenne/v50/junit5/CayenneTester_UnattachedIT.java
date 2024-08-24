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
import io.bootique.jdbc.junit5.derby.DerbyTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@BQTest
public class CayenneTester_UnattachedIT {

    @BQTestTool
    static final DerbyTester db = DerbyTester.db();

    // declare CayenneTester, but don't use it in the app. While wasteful, this should not blow up
    @BQTestTool
    static final CayenneTester cayenne = CayenneTester
            .create()
            .entities(Table1.class)
            .deleteBeforeEachTest();

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique
            .app("-c", "classpath:config2.yml")
            .autoLoadModules()
            .module(db.moduleWithTestDataSource("db"))
            .createRuntime();

    @Test
    @DisplayName("CayenneTester declared, but not attached to the app")
    public void dbAccess() {
        app.getInstance(CayenneRuntime.class).newContext();
    }
}
