package com.nhl.bootique.cayenne;

import com.google.inject.Module;
import com.nhl.bootique.BQModuleProvider;

public class CayenneModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new CayenneModule();
	}
}
