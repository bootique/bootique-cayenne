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
package io.bootique.cayenne.v50.syncfilter;

import org.apache.cayenne.DataChannelSyncFilter;

/**
 * @since 3.0
 */
public class MappedDataChannelSyncFilter {

    private final DataChannelSyncFilter filter;
    private final boolean includeInTransaction;

    public MappedDataChannelSyncFilter(DataChannelSyncFilter filter, boolean includeInTransaction) {
        this.filter = filter;
        this.includeInTransaction = includeInTransaction;
    }

    public DataChannelSyncFilter getFilter() {
        return filter;
    }

    public boolean isIncludeInTransaction() {
        return includeInTransaction;
    }
}
