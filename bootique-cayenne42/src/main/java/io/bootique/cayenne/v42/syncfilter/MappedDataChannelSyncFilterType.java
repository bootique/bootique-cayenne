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
package io.bootique.cayenne.v42.syncfilter;

import org.apache.cayenne.DataChannelSyncFilter;

/**
 * @since 3.0
 */
public class MappedDataChannelSyncFilterType {

    private final Class<? extends DataChannelSyncFilter> filterType;
    private final boolean includeInTransaction;

    public MappedDataChannelSyncFilterType(Class<? extends DataChannelSyncFilter> filterType, boolean includeInTransaction) {
        this.filterType = filterType;
        this.includeInTransaction = includeInTransaction;
    }

    public Class<? extends DataChannelSyncFilter> getFilterType() {
        return filterType;
    }

    public boolean isIncludeInTransaction() {
        return includeInTransaction;
    }
}
