package io.bootique.cayenne.jcache;

import io.bootique.BQRuntime;
import io.bootique.cayenne.CayenneModule;
import io.bootique.jcache.JCacheModule;
import io.bootique.jdbc.JdbcModule;
import io.bootique.test.junit.BQModuleProviderChecker;
import io.bootique.test.junit.BQRuntimeChecker;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

public class CayenneJCacheModuleProviderTest {


    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testAutoLoadable() {
        BQModuleProviderChecker.testAutoLoadable(CayenneJCacheModuleProvider.class);
    }

    @Test
    public void testModuleDeclaresDependencies() {
        final BQRuntime bqRuntime = testFactory.app().module(new CayenneJCacheModuleProvider()).createRuntime();
        BQRuntimeChecker.testModulesLoaded(bqRuntime,
                CayenneModule.class,
                JdbcModule.class,
                JCacheModule.class
        );
    }
}
