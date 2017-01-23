package io.bootique.cayenne.jcache.invalidation;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A Cayenne filter to
 */
// TODO: contribute to Cayenne, replacing its CacheInvalidationFilter ... though Java 8..
public class InvalidationFilter implements DataChannelFilter {

    private final ThreadLocal<Set<String>> groups;
    private final Iterable<InvalidationHandler> handlers;
    private final Map<Class<? extends Persistent>, Function<Persistent, Collection<String>>> mappedHandlers;
    private final Function<Persistent, Collection<String>> skipHandler;
    private QueryCache queryCache;

    public InvalidationFilter(Iterable<InvalidationHandler> handlers, QueryCache queryCache) {
        this.handlers = handlers;
        this.skipHandler = p -> Collections.emptyList();
        this.mappedHandlers = new ConcurrentHashMap<>();
        this.groups = new ThreadLocal<>();
        this.queryCache = queryCache;
    }

    @Override
    public void init(DataChannel channel) {
        // do nothing
    }

    @Override
    public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType, DataChannelFilterChain filterChain) {
        try {
            GraphDiff result = filterChain.onSync(originatingContext, changes, syncType);

            // no exceptions, flush...

            Collection<String> groupSet = groups.get();
            if (groupSet != null && !groupSet.isEmpty()) {
                for (String group : groupSet) {
                    queryCache.removeGroup(group);
                }
            }

            return result;
        } finally {
            groups.set(null);
        }
    }

    /**
     * A callback method that records cache group to flush at the end of the commit.
     */
    @PrePersist
    @PreRemove
    @PreUpdate
    protected void preCommit(Object object) {

        // TODO: for some reason we can't use Persistent as the argument type... (is it fixed in Cayenne 4.0.M4?)
        Persistent p = (Persistent) object;

        Collection<String> objectGroups = mappedHandlers.computeIfAbsent(p.getClass(), type -> {

            for (InvalidationHandler handler : handlers) {

                Optional<Function<Persistent, Collection<String>>> function = handler.canHandle(type);
                if (function.isPresent()) {
                    return function.get();
                }
            }

            return skipHandler;

        }).apply(p);

        if (!objectGroups.isEmpty()) {
            getOrCreateTxGroups().addAll(objectGroups);
        }
    }

    protected Set<String> getOrCreateTxGroups() {
        Set<String> txGroups = groups.get();
        if (txGroups == null) {
            txGroups = new HashSet<>();
            groups.set(txGroups);
        }

        return txGroups;
    }
}
