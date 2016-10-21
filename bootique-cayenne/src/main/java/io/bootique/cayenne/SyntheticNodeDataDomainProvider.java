package io.bootique.cayenne;

import io.bootique.resource.ResourceFactory;
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
import java.util.List;

// TODO: copied from Cayenne, as the corresponding provider is not public or rather
// until https://issues.apache.org/jira/browse/CAY-2095 is implemented
public class SyntheticNodeDataDomainProvider extends DataDomainProvider {

    static final String DEFAULT_NAME = "cayenne";

    @Inject(ServerRuntimeFactory.DATAMAP_CONFIGS_LIST)
    private List<DataMapConfig> dataMapConfigs;

    @Override
    protected DataDomain createAndInitDataDomain() throws Exception {

        DataDomain dataDomain = super.createAndInitDataDomain();

        // add DataMaps that were explicitly configured in BQ config
        if (!dataMapConfigs.isEmpty()) {

            XMLDataMapLoader dataMapLoader = new XMLDataMapLoader();
            for (DataMapConfig dataMapConfig : dataMapConfigs) {

                URL url = new ResourceFactory(dataMapConfig.getLocation()).getUrl();
                String dataMapName = dataMapConfig.getName();
                if (dataMapName == null) {
                    dataMapName = url.toExternalForm();
                }

                Resource location = new URLResource(url);
                DataMap dataMap = dataMapLoader.load(location);

                dataMapConfig.setName(dataMapName);
                dataMap.setName(dataMapName);
                dataDomain.addDataMap(dataMap);

                DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();

                DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor(dataMapName);
                nodeDescriptor.getDataMapNames().add(dataMapName);
                nodeDescriptor.setDataChannelDescriptor(channelDescriptor);

                addDataNode(dataDomain, nodeDescriptor);
            }

            // no nodes... add a synthetic node... it will become the default
        } else if (dataDomain.getDataNodes().isEmpty()) {

            DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();

            DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor(createSyntheticDataNodeName(dataDomain));

            for (DataMap map : dataDomain.getDataMaps()) {
                nodeDescriptor.getDataMapNames().add(map.getName());
            }

            nodeDescriptor.setDataChannelDescriptor(channelDescriptor);

            DataNode node = addDataNode(dataDomain, nodeDescriptor);
            dataDomain.setDefaultNode(node);
        }

        return dataDomain;
    }

    protected String createSyntheticDataNodeName(DataDomain domain) {

        // using Domain's name for the node name.. distinguishing nodes by name
        // may be useful in case of multiple stacks used in the same
        // transaction...

        return domain.getName() != null ? domain.getName() : DEFAULT_NAME;
    }
}

