package com.nhl.bootique.cayenne;

import org.junit.Test;

import com.nhl.bootique.test.junit.BQModuleProviderChecker;

public class CayenneModuleProviderIT {

	@Test
	public void testPresentInJar() {
		BQModuleProviderChecker.testPresentInJar(CayenneModuleProvider.class);
	}
}
