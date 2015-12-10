package com.nhl.launcher.cayenne.datasource;

import javax.sql.DataSource;

public class PoolingDataSourceFactory extends SimpleDataSourceFactory {

	private int minConnections;
	private int maxConnections;

	@Override
	public DataSource toDataSource() {
		return toDataSourceBuilder().pool(minConnections, maxConnections).build();
	}

	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}
}
