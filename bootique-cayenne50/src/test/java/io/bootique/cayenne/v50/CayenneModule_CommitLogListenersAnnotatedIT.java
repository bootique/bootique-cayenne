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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.cayenne.v50.cayenne.T1;
import io.bootique.cayenne.v50.cayenne.T2;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.Confidential;
import org.apache.cayenne.commitlog.model.ChangeMap;
import org.apache.cayenne.commitlog.model.ObjectChange;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@BQTest
public class CayenneModule_CommitLogListenersAnnotatedIT {

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique
            .app("--config=classpath:explicitconfig.yml")
            .autoLoadModules()
            .module(b -> CayenneModule.extend(b)
                    .applyCommitLogAnnotation()
                    .addCommitLogListener(new L1(), true)
                    .addCommitLogListener(L2.class, false))
            .createRuntime();

    @BeforeEach
    void clearListenerState() {
        L1.tracked.clear();
        L2.tracked.clear();
    }

    @Test
    public void listeners() {

        ObjectContext c = app.getInstance(CayenneRuntime.class).newContext();
        T1 t1 = c.newObject(T1.class);
        t1.setName("t1");

        T2 t2 = c.newObject(T2.class);
        t2.setName("t2");

        c.commitChanges();

        assertEquals(Set.of(t2.getObjectId()), L1.tracked.keySet());
        assertEquals(Set.of(t2.getObjectId()), L2.tracked);

        assertSame(
                Confidential.getInstance(),
                L1.tracked.get(t2.getObjectId()).getAttributeChanges().get(T2.NAME.getName()).getNewValue(),
                "Expected confidential name to be hidden");
    }

    static class L1 implements CommitLogListener {

        static Map<ObjectId, ObjectChange> tracked = new HashMap();

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            changes.getChanges().keySet().stream().filter(id -> !id.isTemporary()).forEach(id -> tracked.put(id, changes.getChanges().get(id)));
        }
    }

    static class L2 implements CommitLogListener {

        static Set<ObjectId> tracked = new HashSet<>();

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            changes.getChanges().keySet().stream().filter(id -> !id.isTemporary()).forEach(tracked::add);
        }
    }
}


