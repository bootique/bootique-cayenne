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

package io.bootique.cayenne.v41.test.junit5;

import io.bootique.ModuleExtender;
import io.bootique.cayenne.v41.test.SchemaListener;
import io.bootique.di.Binder;
import io.bootique.di.SetBuilder;

/**
 * @since 2.0
 */
public class CayenneTestJUnit5ModuleExtender extends ModuleExtender<CayenneTestJUnit5ModuleExtender> {

    public CayenneTestJUnit5ModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public CayenneTestJUnit5ModuleExtender initAllExtensions() {
        contributeSchemaListeners();
        return this;
    }

    public CayenneTestJUnit5ModuleExtender addSchemaListener(SchemaListener listener) {
        contributeSchemaListeners().add(listener);
        return this;
    }

    public CayenneTestJUnit5ModuleExtender addSchemaListener(Class<? extends SchemaListener> listenerType) {
        contributeSchemaListeners().add(listenerType);
        return this;
    }

    private SetBuilder<SchemaListener> contributeSchemaListeners() {
        return newSet(SchemaListener.class);
    }
}
