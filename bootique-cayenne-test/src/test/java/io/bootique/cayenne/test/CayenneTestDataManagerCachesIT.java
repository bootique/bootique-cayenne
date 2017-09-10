package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence.Table1;
import io.bootique.cayenne.test.persistence.Table2;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CayenneTestDataManagerCachesIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQRuntime TEST_RUNTIME;

    @Rule
    public CayenneTestDataManager dataManager = CayenneTestDataManager.builder(TEST_RUNTIME)
            .entities(Table1.class, Table2.class)
            .build();

    @BeforeClass
    public static void beforeClass() {
        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:config2.yml")
                .autoLoadModules()
                .createRuntime();
    }

    protected static ServerRuntime getCayenneRuntime() {
        return TEST_RUNTIME.getInstance(ServerRuntime.class);
    }

    @Test
    public void crossTestInterference1() {
        verifyCachesEmptyAndAddObjectsToCache();
    }

    @Test
    public void crossTestInterference2() {
        // do the same thing as "test1" to ensure cross-test interference is verified regardless of the test run order
        verifyCachesEmptyAndAddObjectsToCache();
    }

    private void verifyCachesEmptyAndAddObjectsToCache() {
        // verify that there's no data in the cache
        assertEquals(0, getCayenneRuntime().getDataDomain().getSharedSnapshotCache().size());

        // seed the cache for the next test
        ObjectContext context = getCayenneRuntime().newContext();
        Table1 t1 = context.newObject(Table1.class);
        t1.setA(5L);
        t1.setB(6L);
        context.commitChanges();
    }
}
