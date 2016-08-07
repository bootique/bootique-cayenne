package io.bootique.cayenne;

import io.bootique.test.junit.BQModuleProviderChecker;
import org.junit.Test;

public class CayenneModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(CayenneModuleProvider.class);
	}
}
