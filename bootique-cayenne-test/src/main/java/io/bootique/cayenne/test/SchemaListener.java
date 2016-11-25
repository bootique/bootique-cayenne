package io.bootique.cayenne.test;

import org.apache.cayenne.map.DataMap;

/**
 * A listener that is called when the test DB schema is created.
 *
 * @since 0.13
 */
public interface SchemaListener {

    void afterSchemaCreated(DataMap dataMap);
}
