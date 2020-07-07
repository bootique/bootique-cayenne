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

package io.bootique.cayenne.v42.jcache.invalidation;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.cayenne.v42.jcache.CayenneJCacheModule;
import io.bootique.cayenne.v42.jcache.persistent.Table1;
import io.bootique.cayenne.v42.jcache.persistent.Table2;
import io.bootique.cayenne.v42.junit5.CayenneTester;
import io.bootique.jdbc.junit5.DbTester;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.invalidation.CacheGroupDescriptor;
import org.apache.cayenne.cache.invalidation.CacheGroups;
import org.apache.cayenne.cache.invalidation.InvalidationHandler;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@BQTest
public class CacheInvalidationIT {
    static final InvalidationHandler invalidationHandler =
            type -> type.getAnnotation(CacheGroups.class) == null
                    ? p -> asList(new CacheGroupDescriptor("cayenne1"), new CacheGroupDescriptor("nocayenne1"))
                    : null;

    @BQTestTool
    static final DbTester db = DbTester.derbyDb();

    @BQTestTool
    static final CayenneTester cayenne = CayenneTester
            .create()
            .entities(Table1.class, Table2.class)
            .deleteBeforeEachTest();

    @BQApp(skipRun = true)
    static final BQRuntime runtime = Bootique.app("-c", "classpath:bq1.yml")
            .autoLoadModules()
            .module(b -> CayenneJCacheModule.extend(b).addInvalidationHandler(invalidationHandler))
            .module(db.moduleWithTestDataSource("db"))
            .module(cayenne.moduleWithTestHooks())
            .createRuntime();

    @Test
    public void testInvalidate_CustomHandler() {

        ObjectContext context = cayenne.getRuntime().newContext();
        // no explicit cache group must still work - it lands inside default cache called 'cayenne.default.cache'
        ObjectSelect<Table1> g0 = ObjectSelect.query(Table1.class).localCache();
        ObjectSelect<Table1> g1 = ObjectSelect.query(Table1.class).localCache("cayenne1");
        ObjectSelect<Table1> g2 = ObjectSelect.query(Table1.class).localCache("cayenne2");

        assertEquals(0, g0.select(context).size());
        assertEquals(0, g1.select(context).size());
        assertEquals(0, g2.select(context).size());

        db.getTable(cayenne.getTableName(Table1.class)).insert(1).insert(2);

        // inserted via SQL... query results are still cached...
        assertEquals(0, g0.select(context).size());
        assertEquals(0, g1.select(context).size());
        assertEquals(0, g2.select(context).size());


        Table1 t11 = context.newObject(Table1.class);
        context.commitChanges();

        // inserted via Cayenne... "g1" should get auto refreshed...
        assertEquals(0, g0.select(context).size());
        assertEquals(3, g1.select(context).size());
        assertEquals(0, g2.select(context).size());


        context.deleteObject(t11);
        context.commitChanges();

        // deleted via Cayenne... "g1" should get auto refreshed
        assertEquals(0, g0.select(context).size());
        assertEquals(2, g1.select(context).size());
        assertEquals(0, g2.select(context).size());
    }

    @Test
    public void testInvalidate_CacheGroup() {

        ObjectContext context = cayenne.getRuntime().newContext();
        ObjectSelect<Table2> g3 = ObjectSelect.query(Table2.class).localCache("cayenne3");
        ObjectSelect<Table2> g4 = ObjectSelect.query(Table2.class).localCache("cayenne4");

        assertEquals(0, g3.select(context).size());
        assertEquals(0, g4.select(context).size());

        db.getTable(cayenne.getTableName(Table2.class)).insertColumns("id", "name").values(1, "x1").exec();

        // inserted via SQL... query results are still cached...
        assertEquals(0, g3.select(context).size());
        assertEquals(0, g3.select(context).size());

        Table2 t21 = context.newObject(Table2.class);
        context.commitChanges();

        // inserted via Cayenne... "g1" should get auto refreshed...
        assertEquals(2, g3.select(context).size());
        assertEquals(0, g4.select(context).size());

        context.deleteObject(t21);
        context.commitChanges();

        // deleted via Cayenne... "g1" should get auto refreshed
        assertEquals(1, g3.select(context).size());
        assertEquals(0, g4.select(context).size());
    }

    @Test
    public void testInvalidate_CustomData() {

        ObjectContext context = cayenne.getRuntime().newContext();

        // make sure Cayenne-specific caches are created...
        ObjectSelect<Table1> g1 = ObjectSelect.query(Table1.class).localCache("cayenne1");
        assertEquals(0, g1.select(context).size());

        // add custom data
        CacheManager cacheManager = runtime.getInstance(CacheManager.class);
        Cache<String, String> cache = cacheManager.getCache("cayenne1");
        cache.put("a", "b");

        assertEquals("b", cache.get("a"));

        // generate commit event
        context.newObject(Table1.class);
        context.commitChanges();

        // custom cache entries must expire
        assertNull(cache.get("a"));
    }
}
