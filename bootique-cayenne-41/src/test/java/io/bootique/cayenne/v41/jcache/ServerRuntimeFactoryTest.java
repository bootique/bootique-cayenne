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

package io.bootique.cayenne.v41.jcache;

import io.bootique.cayenne.v41.CayenneConfigMerger;
import io.bootique.cayenne.v41.ServerRuntimeFactory;
import io.bootique.jdbc.DataSourceFactory;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerRuntimeFactoryTest {

	private DataSourceFactory mockDSFactory;
	private CayenneConfigMerger configMerger;

	@Before
	public void before() {
		this.mockDSFactory = mock(DataSourceFactory.class);
		when(mockDSFactory.forName(anyString())).thenReturn(mock(DataSource.class));

		this.configMerger = new CayenneConfigMerger();
	}

	@Test
	public void testCreateCayenneRuntime_NoName() {
		ServerRuntimeFactory factory = new ServerRuntimeFactory();
		factory.setDatasource("ds1");

		ServerRuntime runtime = factory.createCayenneRuntime(
				mockDSFactory,
                configMerger,
				Collections.emptyList(),
				Collections.emptyList());
		try {
			DataDomain domain = runtime.getDataDomain();
			assertEquals("cayenne", domain.getName());

			assertEquals(1, domain.getDataNodes().size());
			assertNotNull(domain.getDefaultNode());
			assertEquals("cayenne", domain.getDefaultNode().getName());

		} finally {
			runtime.shutdown();
		}
	}

	@Test
	public void testCreateCayenneRuntime_Name() {
		ServerRuntimeFactory factory = new ServerRuntimeFactory();
		factory.setConfigs(asList("cayenne-project1.xml"));
		factory.setDatasource("ds1");
		factory.setName("me");

		ServerRuntime runtime = factory.createCayenneRuntime(
				mockDSFactory,
                configMerger,
				Collections.emptyList(),
				Collections.emptyList());
		try {

			DataDomain domain = runtime.getDataDomain();
			assertEquals("me", domain.getName());

			assertEquals(1, domain.getDataNodes().size());
			assertNotNull(domain.getDefaultNode());
			assertEquals("me", domain.getDefaultNode().getName());

		} finally {
			runtime.shutdown();
		}
	}

	@Test
	public void testCreateCayenneRuntime_Configs() {
		ServerRuntimeFactory factory = new ServerRuntimeFactory();
		factory.setDatasource("ds1");
		factory.setConfigs(asList("cayenne-project2.xml", "cayenne-project1.xml"));

		ServerRuntime runtime = factory.createCayenneRuntime(
				mockDSFactory,
                configMerger,
				Collections.emptyList(),
				Collections.emptyList());
		try {

			DataDomain domain = runtime.getDataDomain();
			assertNotNull(domain.getEntityResolver().getDbEntity("db_entity"));
			assertNotNull(domain.getEntityResolver().getDbEntity("db_entity2"));

		} finally {
			runtime.shutdown();
		}
	}

	@Test
	public void testCreateCayenneRuntime_NoConfig() {
		ServerRuntimeFactory factory = new ServerRuntimeFactory();
		factory.setDatasource("ds1");

		ServerRuntime runtime = factory.createCayenneRuntime(
				mockDSFactory,
                configMerger,
				Collections.emptyList(),
				Collections.emptyList());
		try {

			DataDomain domain = runtime.getDataDomain();
			assertTrue(domain.getEntityResolver().getDbEntities().isEmpty());

		} finally {
			runtime.shutdown();
		}
	}
}
