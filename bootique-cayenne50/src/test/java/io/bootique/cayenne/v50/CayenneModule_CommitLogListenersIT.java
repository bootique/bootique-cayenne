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
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.apache.cayenne.*;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.model.ChangeMap;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class CayenneModule_CommitLogListenersIT {

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique
            .app("--config=classpath:genericconfig.yml")
            .autoLoadModules()
            .module(b -> CayenneModule.extend(b)
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

        GenericPersistentObject o1 = new GenericPersistentObject();
        o1.setObjectId(ObjectId.of("T1"));
        o1.writeProperty("name", "n" + 1);

        GenericPersistentObject o2 = new GenericPersistentObject();
        o2.setObjectId(ObjectId.of("T1"));
        o2.writeProperty("name", "n" + 2);

        ObjectContext c = app.getInstance(CayenneRuntime.class).newContext();
        c.registerNewObject(o1);
        c.registerNewObject(o2);
        c.commitChanges();

        assertEquals(Set.of(o1.getObjectId(), o2.getObjectId()), L1.tracked);
        assertEquals(Set.of(o1.getObjectId(), o2.getObjectId()), L2.tracked);

    }

    static class L1 implements CommitLogListener {

        static Set<ObjectId> tracked = new HashSet<>();

        @Override
        public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {
            changes.getChanges().keySet().stream().filter(id -> !id.isTemporary()).forEach(tracked::add);
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


