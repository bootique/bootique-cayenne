/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.cayenne.v50;

import io.bootique.di.BQModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@BQTest
public class DataChannelSyncFilterIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    private CayenneRuntime runtimeWithFilters(DataChannelSyncFilter... filters) {

        BQModule filtersModule = (binder) -> {
            CayenneModuleExtender extender = CayenneModule.extend(binder);
            Arrays.asList(filters).forEach(f -> extender.addSyncFilter(f, false));
        };

        return testFactory.app("--config=classpath:genericconfig.yml")
                .autoLoadModules()
                .module(filtersModule)
                .createRuntime()
                .getInstance(CayenneRuntime.class);
    }

    @Test
    public void onSync() {

        DataChannelSyncFilter f = mock(DataChannelSyncFilter.class);
        when(f.onSync(any(), any(), anyInt(), any())).thenReturn(mock(GraphDiff.class));

        GenericPersistentObject o1 = new GenericPersistentObject();
        o1.setObjectId(ObjectId.of("T1"));
        o1.writeProperty("name", "n" + 1);

        GenericPersistentObject o2 = new GenericPersistentObject();
        o2.setObjectId(ObjectId.of("T1"));
        o2.writeProperty("name", "n" + 2);

        CayenneRuntime runtime = runtimeWithFilters(f);
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
