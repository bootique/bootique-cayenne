package io.bootique.cayenne.test;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.bootique.jdbc.test.JdbcTestModule;
import io.bootique.jdbc.test.runtime.DatabaseChannelFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;

import java.util.Set;

/**
 * An auto-loadable module that installs Cayenne schema generation hooks in bootique-jdbc-test.
 *
 * @since 0.17
 */
public class CayenneTestModule implements Module {

    public static Multibinder<SchemaListener> contributeSchemaListener(Binder binder) {
        return Multibinder.newSetBinder(binder, SchemaListener.class);
    }

    @Override
    public void configure(Binder binder) {

        CayenneTestModule.contributeSchemaListener(binder);

        JdbcTestModule.contributeDataSourceListeners(binder).addBinding().to(SchemaCreationListener.class);

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
