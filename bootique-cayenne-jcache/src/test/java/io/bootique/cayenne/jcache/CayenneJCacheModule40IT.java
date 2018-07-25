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

package io.bootique.cayenne.jcache;

import io.bootique.BQRuntime;
import io.bootique.cayenne.v40.CayenneDomainModuleProvider;
import io.bootique.cayenne.jcache.persistent.Table1;
import io.bootique.cayenne.test.CayenneTestDataManager;
import io.bootique.cayenne.test.CayenneTestModuleProvider;
import io.bootique.jcache.JCacheModuleProvider;
import io.bootique.jdbc.tomcat.JdbcTomcatModuleProvider;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.jcache.JCacheQueryCache;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.cache.CacheManager;

import static org.junit.Assert.*;

public class CayenneJCacheModule40IT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQRuntime TEST_RUNTIME;
    private static ServerRuntime RUNTIME;

    @Rule
    public CayenneTestDataManager dataManager = CayenneTestDataManager.builder(TEST_RUNTIME)
            .entities(Table1.class)
            .build();

    @BeforeClass
    public static void setupRuntime() {
        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:bq1.yml")
                .module(new CayenneDomainModuleProvider())
                .module(new JCacheModuleProvider())
                .module(new JdbcTomcatModuleProvider())
                .module(new CayenneTestModuleProvider())
                .createRuntime();

        RUNTIME = TEST_RUNTIME.getInstance(ServerRuntime.class);
    }

    @Test
    public void testCacheProvider() {
        QueryCache cache = RUNTIME.getInjector().getInstance(QueryCache.class);
        assertTrue("Unexpected cache type: " + cache.getClass().getName(), cache instanceof JCacheQueryCache);
    }

    @Test
    public void testCacheManager() {
        CacheManager cacheManager = RUNTIME.getInjector().getInstance(CacheManager.class);
        assertTrue("Unexpected cache type: " + cacheManager.getClass().getName(),
                cacheManager.getClass().getName().startsWith("org.ehcache.jsr107"));

        CacheManager expectedCacheManager = TEST_RUNTIME.getInstance(CacheManager.class);
        assertSame(expectedCacheManager, cacheManager);
    }

    @Test
    public void testCachedQueries() {

        ObjectContext context = RUNTIME.newContext();
        ObjectSelect<Table1> g1 = ObjectSelect.query(Table1.class).localCache("g1");
        ObjectSelect<Table1> g2 = ObjectSelect.query(Table1.class).localCache("g2");

        dataManager.getTable(Table1.class).insert(1).insert(45);
        assertEquals(2, g1.select(context).size());

        // we are still cached, must not see the new changes
        dataManager.getTable(Table1.class).insert(2).insert(44);
        assertEquals(2, g1.select(context).size());

        // different cache group - must see the changes
        assertEquals(4, g2.select(context).size());

        // refresh the cache, so that "g1" could see the changes
        RUNTIME.getDataDomain().getQueryCache().removeGroup("g1");
        assertEquals(4, g1.select(context).size());
    }
}
