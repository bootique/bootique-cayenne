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
package io.bootique.cayenne.v50.junit5.tester;

import io.bootique.BQRuntime;
import io.bootique.BQRuntimeListener;
import io.bootique.cayenne.v50.CayenneStartupListener;
import io.bootique.di.DIRuntimeException;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQAfterMethodCallback;
import io.bootique.junit5.scope.BQBeforeMethodCallback;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @since 3.0
 */
public class CayenneTesterLifecycleManager implements BQRuntimeListener, CayenneStartupListener, BQBeforeMethodCallback, BQAfterMethodCallback {

    private final Map<Consumer<CayenneRuntime>, CayenneTesterCallbackType> callbacks;
    private BQRuntime bqRuntime;
    private CayenneRuntime cayenneRuntime;
    private boolean withinTestMethod;

    public CayenneTesterLifecycleManager() {
        // ordering of entries is important here
        this.callbacks = new LinkedHashMap<>();
    }

    public BQRuntime getBqRuntime() {
        assertNotNull(bqRuntime, "BQRuntime is not initialized. Not connected to a Bootique runtime?");
        return bqRuntime;
    }

    public CayenneRuntime getCayenneRuntime() {

        if (cayenneRuntime == null) {
            // trigger immediate initialization
            getBqRuntime().getInstance(CayenneRuntime.class);
        }

        assertNotNull(cayenneRuntime, "Unexpected state - CayenneRuntime is not initialized");
        return cayenneRuntime;
    }

    /**
     * Registers a Cayenne startup callback to be run either unconditionally, or only when startup happens within a
     * test method.
     */
    public CayenneTesterLifecycleManager callback(Consumer<CayenneRuntime> callback, CayenneTesterCallbackType type) {
        callbacks.put(callback, type);
        return this;
    }

    // Called by "bootique"
    @Override
    public void onRuntimeCreated(BQRuntime runtime) {
        checkUnused(runtime);
        this.bqRuntime = runtime;
    }

    // Called by "bootique-cayenne"
    @Override
    public void onRuntimeCreated(CayenneRuntime runtime) {
        this.cayenneRuntime = runtime;
        callbacks.forEach((k, v) -> onCayenneStarted(runtime, k, v));
    }

    // Called by "bootique-junit5" via CayenneTester
    @Override
    public void beforeMethod(BQTestScope scope, ExtensionContext context) {
        this.withinTestMethod = true;

        // TODO: prefilter callbacks collection of "beforeTestIfStarted" in a separate collection to avoid iteration
        //  on every run?
        if (isStarted()) {
            callbacks.forEach((k, v) -> beforeTestIfStarted(cayenneRuntime, k, v));
        }
    }

    // Called by "bootique-junit5" via CayenneTester
    @Override
    public void afterMethod(BQTestScope scope, ExtensionContext context) {
        this.withinTestMethod = false;
    }

    private boolean isStarted() {
        return cayenneRuntime != null;
    }

    private void onCayenneStarted(CayenneRuntime runtime, Consumer<CayenneRuntime> callback, CayenneTesterCallbackType type) {

        switch (type) {
            case onCayenneStartup:
                callback.accept(runtime);
                break;
            case beforeTestOrOnCayenneStartupWithinTest:
                if (withinTestMethod) {
                    callback.accept(runtime);
                }
                break;
            default:
                return;
        }
    }

    private void beforeTestIfStarted(CayenneRuntime runtime, Consumer<CayenneRuntime> callback, CayenneTesterCallbackType type) {
        if (type == CayenneTesterCallbackType.beforeTestOrOnCayenneStartupWithinTest) {
            callback.accept(runtime);
        }
    }

    private void checkUnused(BQRuntime runtime) {
        if (this.bqRuntime != null && this.bqRuntime != runtime) {
            throw new DIRuntimeException("BQRuntime is already initialized. " +
                    "Likely this CayenneTester is already connected to another BQRuntime. " +
                    "To fix this error use one CayenneTester per BQRuntime.");
        }
    }
}
