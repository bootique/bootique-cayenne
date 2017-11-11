package io.bootique.cayenne;

import com.google.inject.Module;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.graph.GraphDiff;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CayenneModule_ListenersIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private ServerRuntime runtimeWithListeners(Object... listeners) {

        Module listenersModule = (binder) -> {
            CayenneModuleExtender extender = CayenneModule.extend(binder);
            Arrays.asList(listeners).forEach(extender::addListener);
        };

        return testFactory.app("--config=classpath:genericconfig.yml")
                .autoLoadModules()
                .module(listenersModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);
    }

    private ServerRuntime runtimeWithFilters(DataChannelFilter... filters) {

        Module filtersModule = (binder) -> {

            CayenneModuleExtender extender = CayenneModule.extend(binder);
            Arrays.asList(filters).forEach(extender::addFilter);
        };

        return testFactory.app("--config=classpath:genericconfig.yml")
                .autoLoadModules()
                .module(filtersModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);
    }

    @Test
    public void testListeners() {

        L1 l1 = new L1();

        CayenneDataObject o1 = new CayenneDataObject();
        o1.setObjectId(new ObjectId("T1"));
        o1.writeProperty("name", "n" + 1);

        CayenneDataObject o2 = new CayenneDataObject();
        o2.setObjectId(new ObjectId("T1"));
        o2.writeProperty("name", "n" + 2);

        ServerRuntime runtime = runtimeWithListeners(l1);
        try {
            ObjectContext c = runtime.newContext();
            c.registerNewObject(o1);
            c.registerNewObject(o2);
            c.commitChanges();

        } finally {
            runtime.shutdown();
        }

        assertEquals(2, l1.postPersisted.size());
        assertTrue(l1.postPersisted.contains(o1));
        assertTrue(l1.postPersisted.contains(o2));
    }

    @Test
    public void testFilters() {

        DataChannelFilter f = mock(DataChannelFilter.class);
        when(f.onSync(any(), any(), anyInt(), any())).thenReturn(mock(GraphDiff.class));

        CayenneDataObject o1 = new CayenneDataObject();
        o1.setObjectId(new ObjectId("T1"));
        o1.writeProperty("name", "n" + 1);

        CayenneDataObject o2 = new CayenneDataObject();
        o2.setObjectId(new ObjectId("T1"));
        o2.writeProperty("name", "n" + 2);

        ServerRuntime runtime = runtimeWithFilters(f);
        try {
            ObjectContext c = runtime.newContext();
            c.registerNewObject(o1);
            c.registerNewObject(o2);
            c.commitChanges();

        } finally {
            runtime.shutdown();
        }

        verify(f).onSync(any(), any(), anyInt(), any());
    }
}

class L1 {

    List<Object> postPersisted = new ArrayList<>();

    @PostPersist
    public void postPersist(Object o) {
        postPersisted.add(o);
    }
}
