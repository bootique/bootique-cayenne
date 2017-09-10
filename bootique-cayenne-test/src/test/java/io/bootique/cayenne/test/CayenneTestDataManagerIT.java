package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence.Table1;
import io.bootique.cayenne.test.persistence.Table2;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CayenneTestDataManagerIT {

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

    @Test(expected = IllegalArgumentException.class)
    public void testNoSuchTable() {
        dataManager.getTable(String.class);
    }

    @Test
    public void test1() {

        Table t1 = dataManager.getTable(Table1.class);
        Table t2 = dataManager.getTable(Table2.class);

        assertEquals(0, t1.getRowCount());
        assertEquals(0, t2.getRowCount());

        t1.insert(1, 2, 3);
        t2.insert(5, "x");

        assertEquals(1, t1.getRowCount());
        assertEquals(1, t2.getRowCount());
    }

    @Test
    public void test2() {

        Table t1 = dataManager.getTable(Table1.class);
        Table t2 = dataManager.getTable(Table2.class);

        assertEquals(0, t1.getRowCount());
        assertEquals(0, t2.getRowCount());

        t1.insert(4, 5, 6);
        t2.insert(7, "y");

        assertEquals(1, t1.getRowCount());
        assertEquals(1, t2.getRowCount());
    }
}
