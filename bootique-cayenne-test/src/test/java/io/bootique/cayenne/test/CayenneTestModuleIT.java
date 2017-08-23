package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.test.persistence.Table1;
import io.bootique.cayenne.test.persistence.Table2;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.map.DataMap;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CayenneTestModuleIT {

    @ClassRule
    public static BQTestFactory TEST_FACTORY = new BQTestFactory();
    private static BQRuntime TEST_RUNTIME;

    @Rule
    public CayenneTestDataManager dataManager = new CayenneTestDataManager(TEST_RUNTIME, true, Table1.class, Table2.class);
    private static SchemaListener listener = mock(SchemaListener.class);

    @BeforeClass
    public static void beforeClass() {
        TEST_RUNTIME = TEST_FACTORY.app("-c", "classpath:config2.yml")
                .autoLoadModules()
                .module(b -> {
                    CayenneTestModule.contributeSchemaListener(b).addBinding().toInstance(listener);
                })
                .createRuntime();
    }

    @Test
    public void testSchemaListeners() {
        verify(listener).afterSchemaCreated(any(DataMap.class));
    }

}
