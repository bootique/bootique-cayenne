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
        throw new IllegalStateException("DataSource is not configured. " +
                "Configure it via 'bootique-jdbc' or Cayenne.");
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
        for (DataMapConfig dataMapConfig : dataMapConfigs) {
            if (nodeDescriptor.getName().equals(dataMapConfig.getName())) {
                datasource = dataMapConfig.getDatasource();
                break;
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
