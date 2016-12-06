package io.bootique.cayenne.ehcache.invalidation;

import org.apache.cayenne.Persistent;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * A pluggable handler to invalidate cache groups on changes in certain objects.
 */
public interface InvalidationHandler {

    Optional<Function<Persistent, Collection<String>>> canHandle(Class<? extends Persistent> type);
}
