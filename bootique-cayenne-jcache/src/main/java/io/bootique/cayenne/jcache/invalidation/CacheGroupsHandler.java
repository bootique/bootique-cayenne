package io.bootique.cayenne.jcache.invalidation;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.lifecycle.cache.CacheGroups;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.asList;

public class CacheGroupsHandler implements InvalidationHandler {

    @Override
    public Optional<Function<Persistent, Collection<String>>> canHandle(Class<? extends Persistent> type) {

        CacheGroups a = type.getAnnotation(CacheGroups.class);
        if (a == null) {
            return Optional.empty();
        }

        String[] groups = a.value();
        if (groups == null || groups.length == 0) {
            return Optional.empty();
        }

        Collection<String> groupsList = asList(groups);

        return Optional.of(p -> groupsList);
    }

}
