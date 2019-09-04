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

package io.bootique.cayenne;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: copied from Cayenne, as the corresponding provider is not public or rather
// until https://issues.apache.org/jira/browse/CAY-2095 is implemented
public class SyntheticNodeDataDomainProvider extends DataDomainProvider {

    static final String DEFAULT_NAME = "cayenne";

    @Inject
    private List<DataMapConfig> dataMapConfigs;

    @Inject
    private DefaultDataSourceName defaultDatasource;

    @Override
    protected DataDomain createAndInitDataDomain() throws Exception {

        DataDomain dataDomain = super.createAndInitDataDomain();

        DataNodeDescriptor defaultNodeDescriptor = createDefaultNodeDescriptor(dataDomain);

        // add DataMaps that were explicitly configured in BQ config
        Map<String, Collection<DataMapConfig>> explicitConfigs = getDataMapConfigs();
        if (!explicitConfigs.isEmpty()) {
            explicitConfigs.forEach((datasource, configs) -> mergeConfigs(dataDomain, datasource, configs, defaultNodeDescriptor));
        } else if (dataDomain.getDataNodes().isEmpty()) {
            // no nodes... add a synthetic node... it will become the default
            DataNode defaultNode = addDataNode(dataDomain, defaultNodeDescriptor);
            dataDomain.setDefaultNode(defaultNode);
        }

        return dataDomain;
    }

    private void mergeConfigs(
            DataDomain dataDomain,
            String dataSourceName,
            Collection<DataMapConfig> configs,
            DataNodeDescriptor defaultNodeDescriptor) {

        XMLDataMapLoader dataMapLoader = new XMLDataMapLoader();
        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor(createSyntheticDataNodeName(dataSourceName));
        List<DataChannelDescriptor> channelDescriptors = new ArrayList<>();

        for (DataMapConfig config : configs) {

            URL url = config.getLocation().getUrl();
            String dataMapName = config.getName();
            if (dataMapName == null) {
                dataMapName = url.toExternalForm();
            }

            Resource location = new URLResource(url);
            DataMap dataMap = dataMapLoader.load(location);

            config.setName(dataMapName);
            dataMap.setName(dataMapName);
            dataDomain.addDataMap(dataMap);

            DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();
            channelDescriptor.getDataMaps().add(dataMap);
            channelDescriptor.getNodeDescriptors().add(nodeDescriptor);
            channelDescriptors.add(channelDescriptor);

            nodeDescriptor.getDataMapNames().add(dataMapName);
        }

        if (dataSourceName.equals(defaultDatasource.getOptionalName())
                && !defaultNodeDescriptor.getDataMapNames().isEmpty()) {
            channelDescriptors.add(defaultNodeDescriptor.getDataChannelDescriptor());
            nodeDescriptor.getDataMapNames().addAll(defaultNodeDescriptor.getDataMapNames());
        }

        nodeDescriptor.setDataChannelDescriptor(descriptorMerger.merge(
                channelDescriptors.toArray(new DataChannelDescriptor[channelDescriptors.size()])));

        try {
            addDataNode(dataDomain, nodeDescriptor);
        } catch (Exception e) {

            // TODO: better exception handling
            e.printStackTrace();
        }
    }

    private DataNodeDescriptor createDefaultNodeDescriptor(DataDomain dataDomain) {

        DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();
        DataNodeDescriptor defaultDescriptor = new DataNodeDescriptor(createSyntheticDataNodeName(dataDomain));
        channelDescriptor.getNodeDescriptors().add(defaultDescriptor);

        for (DataMap map : dataDomain.getDataMaps()) {
            channelDescriptor.getDataMaps().add(map);
            defaultDescriptor.getDataMapNames().add(map.getName());
        }

        defaultDescriptor.setDataChannelDescriptor(channelDescriptor);
        return defaultDescriptor;
    }

    protected String createSyntheticDataNodeName(DataDomain domain) {

        // using Domain's name for the node name.. distinguishing nodes by name
        // may be useful in case of multiple stacks used in the same
        // transaction...

        return domain.getName() != null ? domain.getName() : DEFAULT_NAME;
    }

    protected String createSyntheticDataNodeName(String datasource) {
        return datasource + "_node";
    }

    private Map<String, Collection<DataMapConfig>> getDataMapConfigs() {

        if (dataMapConfigs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Collection<DataMapConfig>> result = new HashMap<>();
        for (DataMapConfig dataMapConfig : dataMapConfigs) {

            String datasource = dataMapConfig.getDatasource();
            if (datasource == null) {
                datasource = defaultDatasource.getOptionalName();
            }

            result.computeIfAbsent(datasource, k -> new ArrayList<>()).add(dataMapConfig);
        }

        return result;
    }
}

