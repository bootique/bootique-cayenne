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
import io.bootique.cayenne.v41.test.persistence.Table1;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit5.BQTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CayenneTestDataManagerTxIT {

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDataSourceDoesNotAutocommit() {

        BQRuntime runtime = testFactory.app("-c", "classpath:config-noautocommit.yml")
                .autoLoadModules()
                .createRuntime();

        CayenneTestDataManager dataManager = CayenneTestDataManager.builder(runtime)
                .doNotDeleteData()
                .entities(Table1.class)
                .build();

        Table t1 = dataManager.getTable(Table1.class);

        t1.matcher().assertNoMatches();
        t1.insert(1, 2, 3);
        t1.matcher().assertOneMatch();
    }
}
