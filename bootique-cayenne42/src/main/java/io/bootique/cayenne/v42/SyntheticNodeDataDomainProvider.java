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

package io.bootique.cayenne.v42;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 1.0.RC1
 */
public class SyntheticNodeDataDomainProvider extends DataDomainProvider {

    static final String DEFAULT_NAME = "cayenne";

    @Inject
    protected Provider<DataMapLoader> xmlDataMapLoaderProvider;

    @Inject
    private List<DataMapConfig> dataMapConfigs;

    @Inject
    private DefaultDataSourceName defaultDatasource;

    @Override
    protected DataChannelDescriptor loadDescriptor() {
        DataChannelDescriptor d1 = super.loadDescriptor();
        DataChannelDescriptor d2 = mergeExplicitMaps(d1);
        DataChannelDescriptor d3 = resolveMissingDataNodes(d2);
        return d3;
    }

    private DataChannelDescriptor mergeExplicitMaps(DataChannelDescriptor mainDescriptor) {
        return dataMapConfigs.isEmpty()
                ? mainDescriptor
                : descriptorMerger.merge(mainDescriptor, loadExplicitMaps(mainDescriptor));
    }

    private DataChannelDescriptor loadExplicitMaps(DataChannelDescriptor mainDescriptor) {
        DataChannelDescriptor descriptor = new DataChannelDescriptor();

        // using the same name as main, to preserve the original name during merging
        descriptor.setName(mainDescriptor.getName());

        // clone properties from main to preserve the original properties during merging
        descriptor.getProperties().putAll(mainDescriptor.getProperties());

        Map<String, DataNodeDescriptor> nodeDescriptors = new HashMap<>();

        for (DataMapConfig config : dataMapConfigs) {

            DataMap dataMap = loadDataMap(config);
            descriptor.getDataMaps().add(dataMap);

            String dataSourceName = config.getDatasource() != null
                    ? config.getDatasource()
                    : defaultDatasource.getOptionalName();

            if (dataSourceName != null) {
                createOrUpdateNodeDescriptor(nodeDescriptors, dataMap.getName(), dataSourceName);
            }
        }

        descriptor.getNodeDescriptors().addAll(nodeDescriptors.values());

        return descriptor;
    }

    private DataMap loadDataMap(DataMapConfig config) {

        URL url = config.getLocation().getUrl();
        String dataMapName = config.getName() != null ? config.getName() : url.toExternalForm();
        Resource location = new URLResource(url);
        DataMap dataMap = xmlDataMapLoaderProvider.get().load(location);
        dataMap.setName(dataMapName);

        return dataMap;
    }

    private DataChannelDescriptor resolveMissingDataNodes(DataChannelDescriptor descriptor) {

        // create a DataNode even if there are no maps...
        if (descriptor.getDataMaps().isEmpty()
                && descriptor.getNodeDescriptors().isEmpty()
                && defaultDatasource.getOptionalName() != null) {
            getOrCreateDefaultNodeDescriptor(descriptor);
            return descriptor;
        }

        Set<String> unresolvedMaps = new HashSet<>();
        descriptor.getDataMaps().forEach(dm -> unresolvedMaps.add(dm.getName()));
        descriptor.getNodeDescriptors().forEach(nd -> nd.getDataMapNames().forEach(n -> unresolvedMaps.remove(n)));

        if (unresolvedMaps.size() > 0) {
            getOrCreateDefaultNodeDescriptor(descriptor).getDataMapNames().addAll(unresolvedMaps);
        }

        return descriptor;
    }

    private void createOrUpdateNodeDescriptor(
            Map<String, DataNodeDescriptor> nodes,
            String dataMapName,
            String dataSourceName) {

        // not assigning parent channel to new DataNodeDescriptor, as this object is going to get cloned on merge anyways,
        // with the right final parent assigned..

        nodes
                .computeIfAbsent(dataSourceName, k -> createDataNodeDescriptor(dataSourceName))
                .getDataMapNames()
                .add(dataMapName);
    }

    private DataNodeDescriptor createDataNodeDescriptor(String dataSourceName) {
        DataNodeDescriptor descriptor = new DataNodeDescriptor(createSyntheticDataNodeName(dataSourceName));
        descriptor.setParameters(BQCayenneDataSourceFactory.encodeDataSourceRef(dataSourceName));
        return descriptor;
    }

    private DataNodeDescriptor getOrCreateDefaultNodeDescriptor(DataChannelDescriptor descriptor) {

        if (defaultDatasource.getOptionalName() == null) {
            // TODO: more diagnostics
            throw new IllegalStateException("No default DataSources is available.");
        }

        // search for an existing node that has a matching DS ref
        String parameters = BQCayenneDataSourceFactory.encodeDataSourceRef(defaultDatasource.getOptionalName());
        for (DataNodeDescriptor existing : descriptor.getNodeDescriptors()) {
            if (parameters.equals(existing.getParameters())) {

                // TODO: should we renaming it to follow naming for default Node?
                //  (see TODO on "createSyntheticDataNodeName" though.. this whole thing may be moot eventually)
                return existing;
            }
        }

        String name = createSyntheticDataNodeName(descriptor);

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor(name);
        nodeDescriptor.setDataChannelDescriptor(descriptor);
        nodeDescriptor.setParameters(parameters);

        descriptor.getNodeDescriptors().add(nodeDescriptor);
        descriptor.setDefaultNodeName(name);

        return nodeDescriptor;
    }

    // TODO: in 2.0 simplify the naming.. just use the DS name for nodes. This will be the least confusing approach
    protected String createSyntheticDataNodeName(String datasource) {
        return datasource + "_node";
    }

    // TODO: in 2.0 simplify the naming.. just use the DS name for nodes. This will be the least confusing approach
    protected String createSyntheticDataNodeName(DataChannelDescriptor descriptor) {

        // using Domain's name for the node name.. distinguishing nodes by name
        // may be useful in case of multiple stacks used in the same
        // transaction...

        return descriptor.getName() != null ? descriptor.getName() : DEFAULT_NAME;
    }
}

