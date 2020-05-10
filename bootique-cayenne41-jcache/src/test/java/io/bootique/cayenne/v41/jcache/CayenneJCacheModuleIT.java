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
import io.bootique.Bootique;
import io.bootique.cayenne.v41.jcache.persistent.Table1;
import io.bootique.cayenne.v41.test.CayenneTestDataManager;
import io.bootique.test.junit5.BQApp;
import io.bootique.test.junit5.BQTest;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.jcache.JCacheQueryCache;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class CayenneJCacheModuleIT {


    @BQApp(skipRun = true)
    static final BQRuntime runtime = Bootique.app("-c", "classpath:bq1.yml")
            .autoLoadModules()
            .createRuntime();

    @RegisterExtension
    public CayenneTestDataManager dataManager = CayenneTestDataManager.builder(runtime)
            .entities(Table1.class)
            .build();

    @Test
    public void testCacheProvider() {
        QueryCache cache = dataManager.getRuntime().getInjector().getInstance(QueryCache.class);
        assertTrue(cache instanceof JCacheQueryCache, "Unexpected cache type: " + cache.getClass().getName());
    }

    @Test
    public void testCacheManager() {
        CacheManager cacheManager = runtime.getInstance(CacheManager.class);
        assertTrue(cacheManager.getClass().getName().startsWith("org.ehcache.jsr107"),
                "Unexpected cache type: " + cacheManager.getClass().getName());

        CacheManager expectedCacheManager = runtime.getInstance(CacheManager.class);
        assertSame(expectedCacheManager, cacheManager);
    }

    @Test
    public void testCachedQueries() {

        ObjectContext context = dataManager.getRuntime().newContext();
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
        dataManager.getRuntime().getDataDomain().getQueryCache().removeGroup("g1");
        assertEquals(4, g1.select(context).size());
    }
}
