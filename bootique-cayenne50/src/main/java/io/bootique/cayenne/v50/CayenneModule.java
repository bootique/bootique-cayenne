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

package io.bootique.cayenne.v50;

import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import org.apache.cayenne.runtime.CayenneRuntime;

import jakarta.inject.Singleton;

/**
 * @since 2.0
 */
public class CayenneModule implements BQModule {

    private static final String CONFIG_PREFIX = "cayenne";

    /**
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link CayenneModuleExtender} that can be used to load Cayenne custom extensions.
     */
    public static CayenneModuleExtender extend(Binder binder) {
        return new CayenneModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Apache Cayenne ORM, v4.2")
                .config(CONFIG_PREFIX, CayenneRuntimeFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {
        extend(binder).initAllExtensions();
    }

    @Provides
    @Singleton
    CayenneConfigMerger provideConfigMerger() {
        return new CayenneConfigMerger();
    }

    @Provides
    @Singleton
    CayenneRuntime createCayenneRuntime(ConfigurationFactory configFactory) {
        return configFactory.config(CayenneRuntimeFactory.class, CONFIG_PREFIX).create();
    }
}
