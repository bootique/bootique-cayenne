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
package io.bootique.cayenne.v41.junit5.tester;

/**
 * @deprecated the users are encouraged to switch to Cayenne 4.2
 */
@Deprecated(since = "3.0", forRemoval = true)
public enum CayenneTesterCallbackType {

    // run when Cayenne runtime is started
    onCayenneStartup,

    // run either before each test (when Cayenne is already started) or when Cayenne is started within a test
    beforeTestOrOnCayenneStartupWithinTest;
}
