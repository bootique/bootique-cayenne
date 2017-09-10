package io.bootique.cayenne.test;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ModuleExtender;

/**
 * @since 0.24
 */
public class CayenneTestModuleExtender extends ModuleExtender<CayenneTestModuleExtender> {

    public CayenneTestModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneTestModuleExtender initAllExtensions() {
        contributeSchemaListeners();
        return this;
    }

    public CayenneTestModuleExtender addSchemaListener(SchemaListener listener) {
        contributeSchemaListeners().addBinding().toInstance(listener);
        return this;
    }

    public CayenneTestModuleExtender addSchemaListener(Class<? extends SchemaListener> listenerType) {
        contributeSchemaListeners().addBinding().to(listenerType);
        return this;
    }

    private Multibinder<SchemaListener> contributeSchemaListeners() {
        return newSet(SchemaListener.class);
    }
}
