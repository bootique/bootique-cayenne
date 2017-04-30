package io.bootique.cayenne.jcache;

import io.bootique.BQRuntime;
import io.bootique.cayenne.jcache.cache.TempJCacheQueryCache;
import io.bootique.cayenne.jcache.persistent.Table1;
import io.bootique.cayenne.test.CayenneTestDataManager;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.cache.CacheManager;

import static org.junit.Assert.*;

public class CayenneJCacheModuleIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQRuntime TEST_RUNTIME;
    private static ServerRuntime RUNTIME;

    @Rule
    public CayenneTestDataManager dataManager = new CayenneTestDataManager(TEST_RUNTIME, true, Table1.class);

    @BeforeClass
    public static void setupRuntime() {
        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:bq1.yml")
                .autoLoadModules()
                .createRuntime();

        RUNTIME = TEST_RUNTIME.getInstance(ServerRuntime.class);
    }

    @Test
    public void testCacheProvider() {
        QueryCache cache = RUNTIME.getInjector().getInstance(QueryCache.class);
        // TODO: this should be replaced with JCacheQueryCache once TempJCacheQueryCache is removed
        assertTrue("Unexpected cache type: " + cache.getClass().getName(), cache instanceof TempJCacheQueryCache);
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
