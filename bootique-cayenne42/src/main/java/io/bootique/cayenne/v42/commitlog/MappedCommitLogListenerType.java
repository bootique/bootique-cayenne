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
package io.bootique.cayenne.v42.commitlog;

import org.apache.cayenne.commitlog.CommitLogListener;

/**
 * @since 3.0.M1
 */
public class MappedCommitLogListenerType {

    private final Class<? extends CommitLogListener> listenerType;
    private final boolean includeInTransaction;

    public MappedCommitLogListenerType(Class<? extends CommitLogListener> listenerType, boolean includeInTransaction) {
        this.listenerType = listenerType;
        this.includeInTransaction = includeInTransaction;
    }

    public Class<? extends CommitLogListener> getListenerType() {
        return listenerType;
    }

    public boolean isIncludeInTransaction() {
        return includeInTransaction;
    }
}
