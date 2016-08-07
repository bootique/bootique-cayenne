package io.bootique.cayenne;

import com.google.inject.Module;
import io.bootique.BQModuleProvider;

public class CayenneModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new CayenneModule();
	}
}
