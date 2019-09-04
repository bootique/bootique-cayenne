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

import io.bootique.jdbc.DataSourceFactory;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DelegatingDataSourceFactory;

import javax.sql.DataSource;
import java.util.Collection;

public class BQCayenneDataSourceFactory extends DelegatingDataSourceFactory {

    private static final String PARAM_PREFIX = "bqds:";

    private DataSourceFactory bqDataSourceFactory;
    private String defaultDataSourceName;

    public BQCayenneDataSourceFactory(DataSourceFactory bqDataSourceFactory, String defaultDataSourceName) {
        this.bqDataSourceFactory = bqDataSourceFactory;
        this.defaultDataSourceName = defaultDataSourceName;
    }

    static String encodeDataSourceRef(String bqDataSource) {
        return PARAM_PREFIX + bqDataSource;
    }

    static String decodeDataSourceRef(String ref, String defaultName) {
        return ref != null && ref.startsWith(PARAM_PREFIX) ? ref.substring(PARAM_PREFIX.length()) : defaultName;
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
        return throwOnNoDataSource();
    }

    protected DataSource throwOnNoDataSource() {
        Collection<String> names = bqDataSourceFactory.allNames();
        if (names.isEmpty()) {
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
        String datasource = decodeDataSourceRef(nodeDescriptor.getParameters(), defaultDataSourceName);
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
