package io.bootique.cayenne.jcache;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;
import io.bootique.cayenne.CayenneModuleProvider;
import io.bootique.jcache.JCacheModuleProvider;

import java.util.Collection;

import static java.util.Arrays.asList;

public class CayenneJCacheModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new CayenneJCacheModule();
    }

    @Override
    public Collection<BQModuleProvider> dependencies() {
        return asList(
                new JCacheModuleProvider(),
                new CayenneModuleProvider()
        );
    }
}
