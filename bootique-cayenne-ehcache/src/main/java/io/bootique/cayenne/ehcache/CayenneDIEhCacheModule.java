package io.bootique.cayenne.ehcache;

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

public class CayenneDIEhCacheModule implements Module {

    private QueryCache queryCache;

    public CayenneDIEhCacheModule(QueryCache queryCache) {
        this.queryCache = queryCache;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(QueryCache.class).toInstance(queryCache);
    }
}
