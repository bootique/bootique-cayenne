package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence3.P3T1;
import io.bootique.cayenne.test.persistence3.P3T3;
import io.bootique.cayenne.test.persistence3.P3T4;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit.BQTestFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CayenneTestDataManagerBuilder_DependenciesIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQRuntime TEST_RUNTIME;

    @BeforeClass
    public static void beforeClass() {
        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:config3.yml")
                .autoLoadModules()
                .createRuntime();
    }

    @Test
    public void testDependentEntities1() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(TEST_RUNTIME)
                .entitiesAndDependencies(P3T1.class)
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(4, tables.size());
        assertTrue(tables.contains("p3_t1"));
        assertTrue(tables.contains("p3_t1_t4"));
        assertTrue(tables.contains("p3_t2"));
        assertTrue(tables.contains("p3_t3"));
    }

    @Test
    public void testDependentEntities2() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(TEST_RUNTIME)
                .entitiesAndDependencies(P3T4.class)
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(2, tables.size());
        assertTrue(tables.contains("p3_t4"));
        assertTrue(tables.contains("p3_t1_t4"));
    }

    @Test
    public void testDependentEntities3() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(TEST_RUNTIME)
                .entitiesAndDependencies(P3T3.class)
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(1, tables.size());
        assertTrue(tables.contains("p3_t3"));
    }

    @Test
    public void testDependentTables1() {
        CayenneTestDataManager dm = CayenneTestDataManager
                .builder(TEST_RUNTIME)
                .tablesAndDependencies("p3_t1_t4")
                .build();

        Set<String> tables = Stream.of(dm.getTablesInInsertOrder()).map(Table::getName).collect(Collectors.toSet());
        assertEquals(1, tables.size());
        assertTrue(tables.contains("p3_t1_t4"));
    }
}
