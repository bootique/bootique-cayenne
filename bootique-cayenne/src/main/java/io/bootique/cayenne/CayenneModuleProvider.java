package io.bootique.cayenne;

import com.google.inject.Module;
import io.bootique.BQModule;
import io.bootique.BQModuleProvider;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class CayenneModuleProvider implements BQModuleProvider {

	@Override
	public Module module() {
		return new CayenneModule();
	}

	@Override
	public Map<String, Type> configs() {
		// TODO: config prefix is hardcoded. Refactor away from ConfigModule, and make provider
		// generate config prefix, reusing it in metadata...
		return Collections.singletonMap("cayenne", ServerRuntimeFactory.class);
	}

	@Override
	public BQModule.Builder moduleBuilder() {
		return BQModuleProvider.super
				.moduleBuilder()
				.description("Provides integration with Apache Cayenne.");
	}
}
