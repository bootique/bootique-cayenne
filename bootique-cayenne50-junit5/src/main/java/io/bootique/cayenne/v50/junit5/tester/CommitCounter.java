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
package io.bootique.cayenne.v50.junit5.tester;

import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.graph.GraphDiff;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @since 2.0
 */
public class CommitCounter implements DataChannelSyncFilter {

    private AtomicInteger count;

    public CommitCounter() {
        this.count = new AtomicInteger(0);
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType, DataChannelSyncFilterChain filterChain) {
        count.incrementAndGet();
        return filterChain.onSync(originatingContext, changes, syncType);
    }

    public void assertCount(int expectedCommits) {
        assertEquals(expectedCommits, count.get(), "Unexpected number of Cayenne commits executed");
    }

    public void reset() {
        count.set(0);
    }
}
