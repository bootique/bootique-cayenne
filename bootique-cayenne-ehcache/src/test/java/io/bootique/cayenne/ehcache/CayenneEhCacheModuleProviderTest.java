package io.bootique.cayenne.ehcache;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class CayenneEhCacheModuleProviderTest {

    @Test
    public void testAutoLoadable() {
        BQModuleProviderChecker.testPresentInJar(CayenneEhCacheModuleProvider.class);
    }
}
