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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.model.ChangeMap;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class CayenneModule_CommitLogListenersOrderIT {

    static final List<String> sequence = new ArrayList<>();

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique
            .app("--config=classpath:genericconfig.yml")
            .autoLoadModules()
            .module(b -> CayenneModule.extend(b)
                    .addCommitLogListener(L2.class, true)
                    .addCommitLogListener(new L1(), true, L2.class)
                    .addCommitLogListener(L2.class, false)
                    .addCommitLogListener(L3.class, true, L4.class)
                    .addCommitLogListener(L4.class, true, L1.class)
            )
            .createRuntime();

    @BeforeEach
    void clearListenerState() {
        sequence.clear();
    }

    @Test
    public void testListeners() {

        CayenneDataObject o1 = new CayenneDataObject();
        o1.setObjectId(ObjectId.of("T1"));
        o1.writeProperty("name", "n" + 1);

        ObjectContext c = app.getInstance(ServerRuntime.class).newContext();
        c.registerNewObject(o1);
        c.commitChanges();

        assertEquals("L2:L1:L4:L3:L2", String.join(":", sequence));
    }

    public static class L1 implements CommitLogListener {

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            sequence.add("L1");
        }
    }

    public static class L2 implements CommitLogListener {

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            sequence.add("L2");
        }
    }

    public static class L3 implements CommitLogListener {

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            sequence.add("L3");
        }
    }

    public static class L4 implements CommitLogListener {

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            sequence.add("L4");
        }
    }
}


