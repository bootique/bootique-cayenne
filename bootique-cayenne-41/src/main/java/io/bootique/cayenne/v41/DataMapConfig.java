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

package io.bootique.cayenne.v41;

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
