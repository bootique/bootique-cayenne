package io.bootique.cayenne;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.resource.ResourceFactory;

@BQConfig("Includes a DataMap in the runtime directly, bypassing declaration in 'cayenne-project.xml'.")
public class DataMapConfig {

    private ResourceFactory location;
    private String datasource;
    private String name;

    public ResourceFactory getLocation() {
        return location;
    }

    @BQConfigProperty("DataMap XML file location.")
    public void setLocation(ResourceFactory location) {
        this.location = location;
    }

    public String getDatasource() {
        return datasource;
    }

    @BQConfigProperty("Name of the DataSource to use with DataMap. A DataSource with this name must be defined in" +
            " 'bootique-jdbc' config. If not set, the app use the default DataSource from bootique-jdbc.")
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getName() {
        return name;
    }

    @BQConfigProperty("Assigns a name to the DataMap. If not set, the name will be derived from location.")
    public void setName(String name) {
        this.name = name;
    }
}
