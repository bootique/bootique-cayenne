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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.commitlog.CommitLogFilter;
import org.apache.cayenne.commitlog.CommitLogListener;
import org.apache.cayenne.commitlog.meta.AnnotationCommitLogEntityFactory;
import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;
import org.apache.cayenne.commitlog.meta.IncludeAllCommitLogEntityFactory;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.tx.TransactionFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A helper to conditionally build extensions related to Cayenne CommitLog module.
 *
 * @since 3.0
 */
public class CommitLogModuleBuilder {

    private boolean applyCommitLogAnnotation;
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

    public void applyCommitLogAnnotation() {
        this.applyCommitLogAnnotation = true;
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
            binder.bind(PreTxCommitLogFilter.class).toProviderInstance(new PreTxCommitLogFilterProvider());
            ServerModule
                    .contributeDomainSyncFilters(binder).insertBefore(PreTxCommitLogFilter.class, TransactionFilter.class);
        };

    }

    private Module postTxModule() {

        // reimplementing CommitLogModuleExtender.module() to allow both pre and post commit filters.
        // TODO:  Maybe this should go to Cayenne?

        return binder -> {
            binder.bind(PostTxCommitLogFilter.class).toProviderInstance(new PostTxCommitLogFilterProvider());
            ServerModule
                    .contributeDomainSyncFilters(binder).addAfter(PostTxCommitLogFilter.class, TransactionFilter.class);
        };
    }

    private CommitLogEntityFactory createEntityFactory(Provider<DataChannel> dataChannelProvider) {
        return applyCommitLogAnnotation
                ? new AnnotationCommitLogEntityFactory(dataChannelProvider)
                : new IncludeAllCommitLogEntityFactory();
    }

    // TODO: We need 4 classes to create what are essentially two instances of CommitLogFilter, because the filter
    //  factory code needs access to both this Builder ivars and "Provider<DataChannel> dataChannelProvider" from the
    //  Cayenne injection stack. So aside from moving this logic to Cayenne, Cayenne DI should do a better job bridging
    //  between manually created and DI-created instances.

    class PreTxCommitLogFilterProvider implements Provider<PreTxCommitLogFilter> {

        @Inject
        Provider<DataChannel> dataChannelProvider;

        @Override
        public PreTxCommitLogFilter get() throws DIRuntimeException {
            Objects.requireNonNull(dataChannelProvider);

            return new PreTxCommitLogFilter(
                    createEntityFactory(dataChannelProvider),
                    CommitLogListenerGraph.resolveAndSort(preTx));
        }
    }

    class PostTxCommitLogFilterProvider implements Provider<PostTxCommitLogFilter> {

        @Inject
        Provider<DataChannel> dataChannelProvider;

        @Override
        public PostTxCommitLogFilter get() throws DIRuntimeException {
            Objects.requireNonNull(dataChannelProvider);

            return new PostTxCommitLogFilter(
                    createEntityFactory(dataChannelProvider),
                    CommitLogListenerGraph.resolveAndSort(postTx));
        }
    }

    static class PreTxCommitLogFilter extends CommitLogFilter {

        public PreTxCommitLogFilter(CommitLogEntityFactory entityFactory, List<CommitLogListener> listeners) {
            super(entityFactory, listeners);
        }
    }

    static class PostTxCommitLogFilter extends CommitLogFilter {
        public PostTxCommitLogFilter(CommitLogEntityFactory entityFactory, List<CommitLogListener> listeners) {
            super(entityFactory, listeners);
        }
    }
}
