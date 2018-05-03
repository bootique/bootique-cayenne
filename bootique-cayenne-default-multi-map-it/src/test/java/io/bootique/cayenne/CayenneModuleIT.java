package io.bootique.cayenne;

import io.bootique.cayenne.dm1.Entity1;
import io.bootique.cayenne.dm2.Entity2;
import io.bootique.test.junit.BQTestFactory;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CayenneModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testExplicitMapping() {

        ServerRuntime runtime = testFactory.app("--config=classpath:defaultconfig.yml")
                .autoLoadModules()
                .createRuntime()
                .getInstance(ServerRuntime.class);

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        Set<String> expectedMapNames = new HashSet<>(asList("map1", "map2"));
        Set<String> mapNames = resolver.getDataMaps().stream().map(DataMap::getName).collect(Collectors.toSet());
        assertEquals(expectedMapNames, mapNames);

        assertNotNull(resolver.getDbEntity("db_entity1"));
        assertNotNull(resolver.getDbEntity("db_entity2"));
        assertNotNull(resolver.getObjEntity(Entity1.class));
        assertNotNull(resolver.getObjEntity(Entity2.class));

        ObjectContext context = runtime.newContext();

        context.newObject(Entity1.class);
        context.newObject(Entity2.class);
        context.commitChanges();
    }
}
