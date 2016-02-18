package com.nhl.bootique.cayenne;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.config.ConfigurationFactory;
import com.nhl.bootique.jdbc.DataSourceFactory;
import com.nhl.bootique.log.BootLogger;
import com.nhl.bootique.shutdown.ShutdownManager;

public class CayenneModule_ListenersIT {

	private Module bqMocksModule;

	@Before
	public void before() {
		DataSourceFactory mockDataSourceFactory = mock(DataSourceFactory.class);
		when(mockDataSourceFactory.forName(anyString())).thenReturn(createDataSource());

		BootLogger mockBootLogger = mock(BootLogger.class);
		ShutdownManager mockShutdownManager = mock(ShutdownManager.class);

		ServerRuntimeFactory serverRuntimeFactory = new ServerRuntimeFactory();
		serverRuntimeFactory.setDatasource("ds1");
		serverRuntimeFactory.setCreateSchema(true);

		ConfigurationFactory mockConfigFactory = mock(ConfigurationFactory.class);
		when(mockConfigFactory.config(ServerRuntimeFactory.class, "cayenne")).thenReturn(serverRuntimeFactory);

		this.bqMocksModule = b -> {
			b.bind(ConfigurationFactory.class).toInstance(mockConfigFactory);
			b.bind(DataSourceFactory.class).toInstance(mockDataSourceFactory);
			b.bind(BootLogger.class).toInstance(mockBootLogger);
			b.bind(ShutdownManager.class).toInstance(mockShutdownManager);
		};
	}

	private DataSource createDataSource() {
		// TODO: shut down Derby...
		return DataSourceBuilder.url("jdbc:derby:target/derby/CayenneModule_ListenersIT;create=true")
				.driver("org.apache.derby.jdbc.EmbeddedDriver").build();
	}

	private ServerRuntime createRuntime(Object... listeners) {
		CayenneModule cayenneModule = new CayenneModule().configName("com/nhl/bootique/cayenne/cayenne-generic.xml");

		Module listenersModule = (binder) -> {

			Multibinder<Object> listenersBinder = CayenneModule.contributeListeners(binder);
			Arrays.asList(listeners).forEach(l -> listenersBinder.addBinding().toInstance(l));
		};

		Injector i = Guice.createInjector(cayenneModule, listenersModule, bqMocksModule);

		return i.getInstance(ServerRuntime.class);
	}

	@Test
	public void testListeners() {

		L1 l1 = new L1();

		CayenneDataObject o1 = new CayenneDataObject();
		o1.setObjectId(new ObjectId("T1"));
		o1.writeProperty("name", "n" + 1);

		CayenneDataObject o2 = new CayenneDataObject();
		o2.setObjectId(new ObjectId("T1"));
		o2.writeProperty("name", "n" + 2);

		ServerRuntime runtime = createRuntime(l1);
		try {
			ObjectContext c = runtime.newContext();
			c.registerNewObject(o1);
			c.registerNewObject(o2);
			c.commitChanges();

		} finally {
			runtime.shutdown();
		}

		assertEquals(2, l1.postPersisted.size());
		assertTrue(l1.postPersisted.contains(o1));
		assertTrue(l1.postPersisted.contains(o2));
	}
}

class L1 {

	List<Object> postPersisted = new ArrayList<>();

	@PostPersist
	public void postPersist(Object o) {
		postPersisted.add(o);
	}
}
