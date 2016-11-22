package io.bootique.cayenne;

/**
 * A holder of an optional DefaultDataSource name.
 *
 * @since 0.18
 */
public class DefaultDataSourceName {

    private String optionalName;

    public DefaultDataSourceName(String optionalName) {
        this.optionalName = optionalName;
    }

    public String getOptionalName() {
        return optionalName;
    }
}
