package io.bootique.cayenne.ehcache;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

public class CayenneEhCacheModuleProvider implements BQModuleProvider {

    @Override
    public Module module() {
        return new CayenneEhCacheModule();
    }
}
