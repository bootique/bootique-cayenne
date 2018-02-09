package io.bootique.cayenne.test;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.cayenne.CayenneModuleProvider;
import io.bootique.jdbc.test.JdbcTestModuleProvider;

import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * @since 0.17
 */
public class CayenneTestModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new CayenneTestModule();
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return asList(
                new CayenneModuleProvider(),
                new JdbcTestModuleProvider()
        );
    }
}
