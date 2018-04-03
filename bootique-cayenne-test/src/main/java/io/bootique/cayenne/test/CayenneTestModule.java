package io.bootique.cayenne.test;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.jdbc.JdbcModule;
import io.bootique.jdbc.test.runtime.DatabaseChannelFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;

import java.util.Set;

/**
 * An auto-loadable module that installs Cayenne schema generation hooks in bootique-jdbc-test.
 *
 * @since 0.17
 */
public class CayenneTestModule implements Module {

    /**
     * @param binder Guice DI binder
     * @return a new extender instance.
     * @since 0.24
     */
    public static CayenneTestModuleExtender extend(Binder binder) {
        return new CayenneTestModuleExtender(binder);
    }

    @Override
    public void configure(Binder binder) {
        CayenneTestModule.extend(binder).initAllExtensions();
        JdbcModule.extend(binder).addDataSourceListener(SchemaCreationListener.class);

        // this will trigger eager Cayenne startup and subsequent schema loading in the test DB
        binder.bind(SchemaLoader.class).asEagerSingleton();
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
