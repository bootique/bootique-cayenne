package com.nhl.launcher.cayenne.datasource;

import javax.sql.DataSource;

public class PoolingDataSourceConfig extends SimpleDataSourceConfig {

	private int minConnections;
	private int maxConnections;

	@Override
	public DataSource toDataSource() {
		return toDataSourceBuilder().pool(minConnections, maxConnections).build();
	}

	public int getMinConnections() {
		return minConnections;
	}

	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}
}
