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

import org.apache.cayenne.commitlog.CommitLogFilter;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.meta.IncludeAllCommitLogEntityFactory;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A helper to conditionally build extensions related to Cayenne CommitLog module.
 *
 * @since 3.0.M1
 */
public class CommitLogModuleBuilder {

    private List<MappedCommitLogListener> preTx;
    private List<MappedCommitLogListener> postTx;

    public void appendModules(Collection<Module> modules) {
        if (preTx != null && !preTx.isEmpty()) {
            modules.add(preTxModule());
        }

        if (postTx != null && !postTx.isEmpty()) {
            modules.add(postTxModule());
        }
    }

    public void add(MappedCommitLogListener mappedListener) {
        get(mappedListener.isIncludeInTransaction()).add(mappedListener);
    }

    private List<MappedCommitLogListener> get(boolean includeInTx) {
        return includeInTx ? getPreTx() : getPostTx();
    }

    private List<MappedCommitLogListener> getPreTx() {
        if (preTx == null) {
            preTx = new ArrayList<>();
        }

        return preTx;
    }

    private List<MappedCommitLogListener> getPostTx() {
        if (postTx == null) {
            postTx = new ArrayList<>();
        }

        return postTx;
    }

    private Module preTxModule() {

        // reimplementing CommitLogModuleExtender.module() to allow both pre and post commit filters.
        // TODO:  Maybe this should go to Cayenne?

        return binder -> {
            List<CommitLogListener> listeners = CommitLogListenerGraph.resolveAndSort(preTx);
            CommitLogFilter filter = new CommitLogFilter(new IncludeAllCommitLogEntityFactory(), listeners);
            ServerModule.contributeDomainSyncFilters(binder).insertBefore(filter, TransactionFilter.class);
        };
    }

    private Module postTxModule() {

        // reimplementing CommitLogModuleExtender.module() to allow both pre and post commit filters.
        // TODO:  Maybe this should go to Cayenne?

        return binder -> {
            List<CommitLogListener> listeners = CommitLogListenerGraph.resolveAndSort(postTx);
            CommitLogFilter filter = new CommitLogFilter(new IncludeAllCommitLogEntityFactory(), listeners);
            ServerModule.contributeDomainSyncFilters(binder).addAfter(filter, TransactionFilter.class);
        };
    }
}
