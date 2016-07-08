package com.nhl.bootique.cayenne;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataDomainProvider;
import org.apache.cayenne.map.DataMap;

// TODO: copied from Cayenne, as the corresponding provider is not public or rather
// until https://issues.apache.org/jira/browse/CAY-2095 is implemented
public class SyntheticNodeDataDomainProvider extends DataDomainProvider {

    static final String DEFAULT_NAME = "cayenne";

    @Override
    protected DataDomain createAndInitDataDomain() throws Exception {

        DataDomain dataDomain = super.createAndInitDataDomain();

        // no nodes... add a synthetic node... it will become the default
        if (dataDomain.getDataNodes().isEmpty()) {

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

