package io.bootique.cayenne.test;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class CayenneTestModuleProviderTest {

    @Test
    public void testPresentInJar() {
        BQModuleProviderChecker.testPresentInJar(CayenneTestModuleProvider.class);
    }

    @Test
    public void testMetaData() {
        BQModuleProviderChecker.testMetadata(CayenneTestModuleProvider.class);
    }
}
