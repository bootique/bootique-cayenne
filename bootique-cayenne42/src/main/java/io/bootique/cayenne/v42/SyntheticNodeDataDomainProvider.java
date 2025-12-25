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
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.map.DataMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SyntheticNodeDataDomainProvider extends DataDomainProvider {

    private final String defaultDatasourceName;
    private final Map<String, String> mapsToDatasources;

    public SyntheticNodeDataDomainProvider(String defaultDatasourceName, Map<String, String> mapsToDatasources) {
        this.defaultDatasourceName = defaultDatasourceName;
        this.mapsToDatasources = mapsToDatasources;
    }

    @Override
    protected DataChannelDescriptor loadDescriptor() {
        DataChannelDescriptor d = super.loadDescriptor();
        return updateDataNodes(d);
    }

    private DataChannelDescriptor updateDataNodes(DataChannelDescriptor cd) {

        // create a DataNode even if there are no maps...
        if (cd.getDataMaps().isEmpty() && cd.getNodeDescriptors().isEmpty() && defaultDatasourceName != null) {
            addNodeDescriptor(cd, defaultDatasourceName, true);
            return cd;
        }

        Map<String, DataNodeDescriptor> bqDataNodes = new HashMap<>();

        // all maps present in "mapsToDatasources" must get the exact requested DataNodes
        for (Map.Entry<String, String> e : mapsToDatasources.entrySet()) {
            DataMap dm = cd.getDataMap(e.getKey());
            if (dm != null) {

                // Note that keys in "bqDataNodes" may be different from the created "DataNodeDescriptor.name".
                // This is why we need a local map to see which ones need to be created
                DataNodeDescriptor nd = bqDataNodes.computeIfAbsent(e.getValue(), ds -> addNodeDescriptor(cd, ds, false));
                relinkDataMapToNode(cd, e.getKey(), nd.getName());
            }
        }

        // any remaining stray maps should be linked to the default DataSource if it exists
        if (defaultDatasourceName != null) {
            Set<String> unresolvedMaps = new HashSet<>();
            cd.getDataMaps().forEach(dm -> unresolvedMaps.add(dm.getName()));
            cd.getNodeDescriptors().forEach(nd -> nd.getDataMapNames().forEach(unresolvedMaps::remove));

            if (!unresolvedMaps.isEmpty()) {
                DataNodeDescriptor nd = bqDataNodes.computeIfAbsent(defaultDatasourceName, ds -> addNodeDescriptor(cd, ds, true));
                unresolvedMaps.forEach(mn -> relinkDataMapToNode(cd, mn, nd.getName()));
            }
        }

        // any unlinked DataNodes should be removed
        // TODO: Those are likely the nodes abandoned due to explicit relinking of their DataMaps. Is this too invasive
        //  to get rid of them?
        cd.getNodeDescriptors().removeIf(nd -> nd.getDataMapNames().isEmpty());

        return cd;
    }

    private DataNodeDescriptor addNodeDescriptor(DataChannelDescriptor cd, String dataSourceName, boolean defaultNode) {

        Objects.requireNonNull(dataSourceName);

        // Using Domain's name for the node name. Distinguishing nodes by name may be useful in case of multiple
        // stacks used in the same transaction...

        String name = uniqueNodeName(cd, dataSourceName);

        DataNodeDescriptor nd = new DataNodeDescriptor(name);
        nd.setDataChannelDescriptor(cd);
        nd.setParameters(BQCayenneDataSourceFactory.encodeDataSourceRef(dataSourceName));

        cd.getNodeDescriptors().add(nd);

        if (defaultNode) {
            cd.setDefaultNodeName(name);
        }

        return nd;
    }

    private void relinkDataMapToNode(DataChannelDescriptor cd, String dataMapName, String targetDataNodeName) {
        DataNodeDescriptor targetNd = cd.getNodeDescriptor(targetDataNodeName);
        cd.getNodeDescriptors().stream().filter(nd -> nd != targetNd).forEach(nd -> nd.getDataMapNames().remove(dataMapName));
        if (!targetNd.getDataMapNames().contains(dataMapName)) {
            targetNd.getDataMapNames().add(dataMapName);
        }
    }

    private String uniqueNodeName(DataChannelDescriptor cd, String prefix) {

        if (cd.getNodeDescriptor(prefix) == null) {
            return prefix;
        }

        int attempts = cd.getNodeDescriptors().size() + 1;
        for (int i = 1; i <= attempts; i++) {
            String name = prefix + i;
            if (cd.getNodeDescriptor(name) == null) {
                return name;
            }
        }

        throw new IllegalStateException("Failed to generate a unique node name for prefix: " + prefix);
    }
}

