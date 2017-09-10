package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence.Table1;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CayenneTestDataManagerTxIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    @Ignore
    // TODO: unignore when #44 is fixed by upgrading to Cayenne 4.0.B2
    public void testDataSourceDoesNotAutocommit() {

        BQRuntime runtime = testFactory.app("-c", "classpath:config-noautocommit.yml")
                .autoLoadModules()
                .createRuntime();

        CayenneTestDataManager dataManager = CayenneTestDataManager.builder(runtime)
                .doNotDeleteData()
                .entities(Table1.class)
                .build();

        Table t1 = dataManager.getTable(Table1.class);

        assertEquals(0, t1.getRowCount());
        t1.insert(1, 2, 3);
        assertEquals(1, t1.getRowCount());
    }
}
