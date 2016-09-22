package io.bootique.cayenne.test;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

/**
 * @since 0.17
 */
public class CayenneTestModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new CayenneTestModule();
    }
}
