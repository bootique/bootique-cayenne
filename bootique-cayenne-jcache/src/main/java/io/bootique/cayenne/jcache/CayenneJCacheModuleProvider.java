package io.bootique.cayenne.jcache;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

public class CayenneJCacheModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new CayenneJCacheModule();
    }
}
