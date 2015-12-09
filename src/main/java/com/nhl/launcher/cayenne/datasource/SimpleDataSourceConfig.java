package com.nhl.launcher.cayenne.datasource;

import javax.sql.DataSource;

import org.apache.cayenne.datasource.DataSourceBuilder;

public class SimpleDataSourceConfig {

	private String url;
	private String driver;

	private String username;
	private String password;

	public DataSource toDataSource() {
		return toDataSourceBuilder().build();
	}

	protected DataSourceBuilder toDataSourceBuilder() {
		return DataSourceBuilder.url(url).driver(driver).userName(username).password(password);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
