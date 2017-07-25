package io.bootique.cayenne.test;

import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.map.DataMap;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CayenneTestModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    @Ignore
    public void testSchemaListeners() {

        SchemaListener listener = mock(SchemaListener.class);

        testFactory.app("-c", "classpath:config2.yml")
                .autoLoadModules()
                .module(b -> {
                    CayenneTestModule.contributeSchemaListener(b).addBinding().toInstance(listener);
                })
                .createRuntime();


        verify(listener).afterSchemaCreated(any(DataMap.class));
    }

}
