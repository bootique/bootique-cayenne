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
package io.bootique.cayenne.v41;

import io.bootique.BQRuntime;
import io.bootique.di.BQModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class CayenneStartupListenerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    private BQRuntime runtimeWithCallbacks(CayenneStartupListener... callbacks) {

        BQModule callbacksModule = (binder) -> {
            CayenneModuleExtender extender = CayenneModule.extend(binder);
            Arrays.asList(callbacks).forEach(extender::addStartupListener);
        };

        return testFactory.app()
                .autoLoadModules()
                .module(callbacksModule)
                .createRuntime();
    }

    @Test
    public void testOnSync() {

        TestStartupListener l1 = new TestStartupListener();
        TestStartupListener l2 = new TestStartupListener();

        BQRuntime rt = runtimeWithCallbacks(l1, l2);

        // lazy init... Cayenne stack won't start until someone requests ServerRuntime
        assertFalse(l1.invoked);
        assertFalse(l2.invoked);

        rt.getInstance(ServerRuntime.class);

        assertTrue(l1.invoked);
        assertTrue(l2.invoked);
    }

    static final class TestStartupListener implements CayenneStartupListener {

        boolean invoked;

        @Override
        public void onRuntimeCreated(ServerRuntime runtime) {
            assertNotNull(runtime);
            this.invoked = true;
        }
    }
}
