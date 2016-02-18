package com.nhl.bootique.cayenne;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.nhl.bootique.jdbc.DataSourceFactory;
import com.nhl.bootique.log.BootLogger;
import com.nhl.bootique.shutdown.ShutdownManager;

public class CayenneModule_ListenersIT {

	private DataSourceFactory mockDataSourceFactory;
	private BootLogger mockBootLogger;
	private ShutdownManager mockShutdownManager;
	private ServerRuntimeFactory serverRuntimeFactory;
	private Module bqMocksModule;

	@Before
	public void before() {
		this.mockDataSourceFactory = mock(DataSourceFactory.class);
		this.mockBootLogger = mock(BootLogger.class);
		this.mockShutdownManager = mock(ShutdownManager.class);

		this.serverRuntimeFactory = new ServerRuntimeFactory();
		serverRuntimeFactory.setDatasource("ds1");

		this.bqMocksModule = b -> {
			b.bind(DataSourceFactory.class).toInstance(mockDataSourceFactory);
			b.bind(BootLogger.class).toInstance(mockBootLogger);
			b.bind(ShutdownManager.class).toInstance(mockShutdownManager);
		};
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
