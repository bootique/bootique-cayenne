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

package io.bootique.cayenne.v41.jcache;

import io.bootique.BQRuntime;
import io.bootique.cayenne.v41.CayenneModule;
import io.bootique.jcache.JCacheModule;
import io.bootique.jdbc.JdbcModule;
import io.bootique.junit5.*;
import org.junit.jupiter.api.Test;

@BQTest
public class CayenneJCacheModuleProviderTest {

    @BQTestTool
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void autoLoadable() {
        BQModuleProviderChecker.testAutoLoadable(CayenneJCacheModuleProvider.class);
    }

    @Test
    public void moduleDeclaresDependencies() {
        BQRuntime bqRuntime = testFactory.app().moduleProvider(new CayenneJCacheModuleProvider()).createRuntime();
        BQRuntimeChecker.testModulesLoaded(bqRuntime,
                CayenneModule.class,
                JdbcModule.class,
                JCacheModule.class
        );
    }
}
