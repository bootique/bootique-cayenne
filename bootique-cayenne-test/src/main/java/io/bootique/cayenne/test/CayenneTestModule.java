package io.bootique.cayenne.test;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.jdbc.test.JdbcTestModule;
import org.apache.cayenne.configuration.server.ServerRuntime;

/**
 * An auto-loadable module that installs Cayenne schema generation hooks in bootique-jdbc-test.
 *
 * @since 0.17
 */
public class CayenneTestModule implements Module {

    @Override
    public void configure(Binder binder) {
        JdbcTestModule.contributeDataSourceListeners(binder).addBinding().to(SchemaCreationListener.class);

        // this will trigger eager Cayenne startup and subsequent schema loading in the test DB
        binder.bind(SchemaLoader.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    SchemaCreationListener provideSchemaCreationListener() {
        return new SchemaCreationListener();
    }

    static class SchemaLoader {

        @Inject
        public SchemaLoader(SchemaCreationListener schemaCreationListener, ServerRuntime runtime) {
            schemaCreationListener.createSchemas(runtime);
        }
    }
}
