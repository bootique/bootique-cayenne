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

package io.bootique.cayenne.v41.test;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jdbc.JdbcModule;
import io.bootique.jdbc.test.runtime.DatabaseChannelFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;

/**
 * An auto-loadable module that installs Cayenne schema generation hooks in bootique-jdbc-test.
 *
 * @deprecated phasing out JUnit 4 support in favor of JUnit 5, same for Cayenne 4.1 in favor of 4.2
 */
@Deprecated(since = "3.0", forRemoval = true)
public class CayenneTestModule implements BQModule {

    /**
     * @param binder Bootique DI binder
     * @return a new extender instance.
     */
    public static CayenneTestModuleExtender extend(Binder binder) {
        return new CayenneTestModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Deprecated, can be replaced with 'bootique-cayenne42-junit5'.")
                .build();
    }

    @Override
    public void configure(Binder binder) {
        CayenneTestModule.extend(binder).initAllExtensions();
        JdbcModule.extend(binder).addDataSourceListener(SchemaCreationListener.class);

        // this will trigger eager Cayenne startup and subsequent schema loading in the test DB
        binder.bind(SchemaLoader.class).initOnStartup();
    }

    @Provides
    @Singleton
    CayenneTableManager provideCayenneTableManager(DatabaseChannelFactory channelFactory, ServerRuntime runtime) {
        // TODO: only works with a single DatabaseChannel per runtime
        return new CayenneTableManager(runtime.getDataDomain().getEntityResolver(), channelFactory.getChannel());
    }

    @Provides
    @Singleton
    SchemaCreationListener provideSchemaCreationListener(Set<SchemaListener> schemaListeners) {
        return new SchemaCreationListener(schemaListeners);
    }

    static class SchemaLoader {

        @Inject
        public SchemaLoader(SchemaCreationListener schemaCreationListener, ServerRuntime runtime) {
            schemaCreationListener.createSchemas(runtime);
        }
    }
}
