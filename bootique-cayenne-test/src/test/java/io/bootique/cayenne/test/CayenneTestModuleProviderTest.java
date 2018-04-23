package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import io.bootique.cayenne.CayenneModule;
import io.bootique.jdbc.JdbcModule;
import io.bootique.jdbc.test.JdbcTestModule;
import io.bootique.jdbc.tomcat.JdbcTomcatModuleProvider;
import io.bootique.test.junit.BQModuleProviderChecker;
import io.bootique.test.junit.BQRuntimeChecker;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

public class CayenneTestModuleProviderTest {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testAutoLoadable() {
        BQModuleProviderChecker.testAutoLoadable(CayenneTestModuleProvider.class);
    }

    @Test
    public void testModuleDeclaresDependencies() {
        final BQRuntime bqRuntime = testFactory
                // add arguments and tomcat module,
                // since DataSource required for CayenneTestModule
                .app("-c", "classpath:config4.yml")
                .module(new JdbcTomcatModuleProvider())
                .module(new CayenneTestModuleProvider())
                .createRuntime();

        BQRuntimeChecker.testModulesLoaded(bqRuntime,
                JdbcModule.class,
                JdbcTestModule.class,
                CayenneModule.class,
                CayenneTestModule.class
        );
    }
}
