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

import io.bootique.BQModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelQueryFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class DataChannelQueryFilterIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    private ServerRuntime runtimeWithFilters(DataChannelQueryFilter... filters) {

        BQModule filtersModule = (binder) -> {
            CayenneModuleExtender extender = CayenneModule.extend(binder);
            Arrays.asList(filters).forEach(extender::addQueryFilter);
        };

        return testFactory.app("--config=classpath:genericconfig.yml")
                .autoLoadModules()
                .module(filtersModule)
                .createRuntime()
                .getInstance(ServerRuntime.class);
    }

    @Test
    public void onQuery() {

        TestFilter f = new TestFilter();

        ServerRuntime runtime = runtimeWithFilters(f);
        try {
            ObjectSelect.dbQuery("T1").select(runtime.newContext());
        } finally {
            runtime.shutdown();
        }

        assertTrue(f.onQueryCalled);
    }

    static class TestFilter implements DataChannelQueryFilter {
        boolean onQueryCalled;

        @Override
        public QueryResponse onQuery(ObjectContext objectContext, Query query, DataChannelQueryFilterChain dataChannelQueryFilterChain) {
            onQueryCalled = true;
            return dataChannelQueryFilterChain.onQuery(objectContext, query);
        }
    }
}
