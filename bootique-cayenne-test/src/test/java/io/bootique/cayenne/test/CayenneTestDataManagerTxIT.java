package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence.Table1;
import io.bootique.jdbc.test.Table;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

public class CayenneTestDataManagerTxIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testDataSourceDoesNotAutocommit() {

        BQRuntime runtime = testFactory.app("-c", "classpath:config-noautocommit.yml")
                .autoLoadModules()
                .createRuntime();

        CayenneTestDataManager dataManager = CayenneTestDataManager.builder(runtime)
                .doNotDeleteData()
                .entities(Table1.class)
                .build();

        Table t1 = dataManager.getTable(Table1.class);

        t1.matcher().assertNoMatches();
        t1.insert(1, 2, 3);
        t1.matcher().assertOneMatch();
    }
}
