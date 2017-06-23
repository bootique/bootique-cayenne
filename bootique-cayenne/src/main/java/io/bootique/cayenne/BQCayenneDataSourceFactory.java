package io.bootique.cayenne;

import io.bootique.jdbc.DataSourceFactory;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DelegatingDataSourceFactory;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

public class BQCayenneDataSourceFactory extends DelegatingDataSourceFactory {

    private DataSourceFactory bqDataSourceFactory;
    private String defaultDataSourceName;
    private List<DataMapConfig> dataMapConfigs;

    public BQCayenneDataSourceFactory(DataSourceFactory bqDataSourceFactory,
                                      String defaultDataSourceName,
                                      List<DataMapConfig> dataMapConfigs) {

        this.bqDataSourceFactory = bqDataSourceFactory;
        this.defaultDataSourceName = defaultDataSourceName;
        this.dataMapConfigs = dataMapConfigs;
    }

    @Override
    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

        DataSource dataSource;

        // 1. try DataSource explicitly mapped in Bootique
        dataSource = mappedBootiqueDataSource(nodeDescriptor);
        if (dataSource != null) {
            return dataSource;
        }

        // 2. try loading from Cayenne XML
        dataSource = cayenneDataSource(nodeDescriptor);
        if (dataSource != null) {
            return dataSource;
        }

        // 3. try default DataSource from Bootique
        dataSource = defaultBootiqueDataSource();
        if (dataSource != null) {
            return dataSource;
        }

        // 4. throw
        return throwOnNoDataSource(nodeDescriptor);
    }

    protected DataSource throwOnNoDataSource(DataNodeDescriptor nodeDescriptor) {
        Collection<String> names = bqDataSourceFactory.allNames();
        if (names.isEmpty()) {
            if (nodeDescriptor.getConfigurationSource() == null) {
                throw new IllegalArgumentException("No DataSources are available for Cayenne:\n" +
                        "1.Default configuration resource \"cayenne-project.xml\" is not found or doesn't contain DataSource.\n" +
                        "  Check Cayenne project location or add data node." + "\n" +
                        "2.Resource with non-default name isn't contributed into CayenneModule via extender.\n" +
                        "  Check module configuration.");
            }
            throw new IllegalStateException("No DataSources are available for Cayenne. " +
                    "Add a DataSource via 'bootique-jdbc' or map it in Cayenne project.");
        }

        if (defaultDataSourceName == null) {
            throw new IllegalStateException(
                    String.format("Can't map Cayenne DataSource: 'cayenne.datasource' is missing. " +
                            "Available DataSources are %s", names));
        }

        throw new IllegalStateException(
                String.format("Can't map Cayenne DataSource: 'cayenne.datasource' is set to '%s'. " +
                        "Available DataSources: %s", defaultDataSourceName, names));
    }

    protected DataSource cayenneDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

        // trying to guess whether Cayenne will be able to provide a DataSource without our help...
        if (shouldConfigureDataSourceFromProperties(nodeDescriptor)
                || nodeDescriptor.getDataSourceFactoryType() != null
                || nodeDescriptor.getDataSourceDescriptor() != null) {

            return super.getDataSource(nodeDescriptor);
        }

        return null;
    }

    protected DataSource defaultBootiqueDataSource() {
        Collection<String> names = bqDataSourceFactory.allNames();
        if (names.size() == 1) {
            return mappedBootiqueDataSource(names.iterator().next());
        }

        return null;
    }

    protected DataSource mappedBootiqueDataSource(DataNodeDescriptor nodeDescriptor) {

        String datasource = null;
        if (!nodeDescriptor.getDataMapNames().isEmpty()) {
            String dataMapName = nodeDescriptor.getDataMapNames().iterator().next();
            for (DataMapConfig dataMapConfig : dataMapConfigs) {
                if (dataMapName.equals(dataMapConfig.getName())) {
                    datasource = dataMapConfig.getDatasource();
                    break;
                }
            }
        }

        if (datasource == null) {
            datasource = defaultDataSourceName;
        }

        return mappedBootiqueDataSource(datasource);
    }

    protected DataSource mappedBootiqueDataSource(String datasource) {

        if (datasource == null) {
            return null;
        }

        DataSource ds = bqDataSourceFactory.forName(datasource);
        if (ds == null) {
            throw new IllegalStateException("Unknown 'defaultDataSourceName': " + datasource);
        }

        return ds;
    }
}
