package io.bootique.cayenne;

import java.util.Collection;
import java.util.Objects;

/**
 * A simple merger that uses "last wins" strategy, returning the last collection passed to the
 * method.
 *
 * @since 0.18
 */
public class CayenneConfigMerger {

    public Collection<String> merge(Collection<String> configs1,
                                    Collection<String> configs2) {
        return configs2 == null || configs2.isEmpty() ? Objects.requireNonNull(configs1) : configs2;
    }
}
