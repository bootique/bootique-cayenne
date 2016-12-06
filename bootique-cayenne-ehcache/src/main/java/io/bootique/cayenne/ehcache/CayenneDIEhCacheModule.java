package io.bootique.cayenne.ehcache;

import io.bootique.cayenne.ehcache.invalidation.InvalidationFilter;
import io.bootique.cayenne.ehcache.invalidation.InvalidationHandler;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionFilter;

import java.util.Set;

public class CayenneDIEhCacheModule implements Module {

    private QueryCache queryCache;
    private Set<InvalidationHandler> invalidationHandlers;

    public CayenneDIEhCacheModule(QueryCache queryCache, Set<InvalidationHandler> invalidationHandlers) {
        this.queryCache = queryCache;
        this.invalidationHandlers = invalidationHandlers;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(QueryCache.class).toInstance(queryCache);

        if (!invalidationHandlers.isEmpty()) {
            InvalidationFilter filter = new InvalidationFilter(invalidationHandlers, queryCache);

            // want the filter to be INSIDE transaction
            binder.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST).add(filter).before(TransactionFilter.class);
        }
    }
}
