package io.bootique.cayenne.jcache;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class CayenneJCacheModuleProviderTest {

    @Test
    public void testAutoLoadable() {
        BQModuleProviderChecker.testPresentInJar(CayenneJCacheModuleProvider.class);
    }
}
