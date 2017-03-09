package io.bootique.cayenne.jcache.invalidation;

import io.bootique.cayenne.jcache.CayenneJCacheModule;
import io.bootique.cayenne.jcache.persistent.Table1;
import io.bootique.cayenne.jcache.persistent.Table2;
import io.bootique.cayenne.test.CayenneTestDataManager;
import io.bootique.test.BQTestRuntime;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.lifecycle.cache.CacheGroups;
import org.apache.cayenne.lifecycle.cache.InvalidationHandler;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class CacheInvalidationIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQTestRuntime TEST_RUNTIME;
    private static ServerRuntime SERVER_RUNTIME;

    @Rule
    public CayenneTestDataManager dataManager = new CayenneTestDataManager(TEST_RUNTIME, true, Table1.class, Table2.class);

    @BeforeClass
    public static void beforeClass() {

        InvalidationHandler invalidationHandler = type -> {
            if(type.getAnnotation(CacheGroups.class) != null) {
                return null;
            }

            return p -> asList("cayenne1");
        };

        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:bq1.yml")
                .autoLoadModules()
                .module(b -> CayenneJCacheModule.extend(b).addInvalidationHandler(invalidationHandler))
                .createRuntime();
        SERVER_RUNTIME = TEST_RUNTIME.getRuntime().getInstance(ServerRuntime.class);
    }

    @Test
    public void testInvalidate_Custom() {

        ObjectContext context = SERVER_RUNTIME.newContext();
        // no explicit cache group must still work - it lands inside default cache called 'cayenne.default.cache'
        ObjectSelect<Table1> g0 = ObjectSelect.query(Table1.class).localCache();
        ObjectSelect<Table1> g1 = ObjectSelect.query(Table1.class).localCache("cayenne1");
        ObjectSelect<Table1> g2 = ObjectSelect.query(Table1.class).localCache("cayenne2");

        assertEquals(0, g0.select(context).size());
        assertEquals(0, g1.select(context).size());
        assertEquals(0, g2.select(context).size());

        dataManager.getTable(Table1.class).insert(1).insert(2);

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

        ObjectContext context = SERVER_RUNTIME.newContext();
        ObjectSelect<Table2> g3 = ObjectSelect.query(Table2.class).localCache("cayenne3");
        ObjectSelect<Table2> g4 = ObjectSelect.query(Table2.class).localCache("cayenne4");

        assertEquals(0, g3.select(context).size());
        assertEquals(0, g4.select(context).size());

        dataManager.getTable(Table2.class).insertColumns("id", "name").values(1, "x1").exec();

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
}
