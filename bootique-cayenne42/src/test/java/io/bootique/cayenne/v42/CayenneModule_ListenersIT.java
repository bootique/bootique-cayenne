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

package io.bootique.cayenne.v42;

import io.bootique.di.BQModule;
import io.bootique.junit5.BQTestFactory;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CayenneModule_ListenersIT {

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory();

    private ServerRuntime runtimeWithListeners(Object... listeners) {

        BQModule listenersModule = (binder) -> {
            CayenneModuleExtender extender = CayenneModule.extend(binder);
            Arrays.asList(listeners).forEach(extender::addListener);
        };

        return testFactory.app("--config=classpath:genericconfig.yml")
                .autoLoadModules()
                .module(listenersModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);
    }

    @Test
    public void testListeners() {

        L1 l1 = new L1();

        CayenneDataObject o1 = new CayenneDataObject();
        o1.setObjectId(ObjectId.of("T1"));
        o1.writeProperty("name", "n" + 1);

        CayenneDataObject o2 = new CayenneDataObject();
        o2.setObjectId(ObjectId.of("T1"));
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

    static class L1 {

        List<Object> postPersisted = new ArrayList<>();

        @PostPersist
        public void postPersist(Object o) {
            postPersisted.add(o);
        }
    }
}


