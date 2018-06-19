/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
