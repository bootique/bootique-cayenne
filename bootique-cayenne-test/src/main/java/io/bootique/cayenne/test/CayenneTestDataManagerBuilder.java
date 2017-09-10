package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @since 0.24
 */
public class CayenneTestDataManagerBuilder {
    private BQRuntime runtime;
    private boolean deleteData;
    private boolean refreshCayenneCaches;
    private List<DbEntity> entities;
    private EntityResolver resolver;

    CayenneTestDataManagerBuilder(BQRuntime runtime) {
        this.runtime = runtime;
        this.deleteData = true;
        this.refreshCayenneCaches = true;

        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        this.resolver = serverRuntime.getDataDomain().getEntityResolver();
        this.entities = new ArrayList<>();
    }

    public CayenneTestDataManager build() {
        return new CayenneTestDataManager(
                runtime.getInstance(ServerRuntime.class).getDataDomain(),
                runtime.getInstance(CayenneTableManager.class),
                deleteData,
                refreshCayenneCaches,
                CayenneModelUtils.tablesInInsertOrder(runtime, entities));
    }

    public CayenneTestDataManagerBuilder doNotDeleteData() {
        this.deleteData = false;
        return this;
    }

    public CayenneTestDataManagerBuilder doNoRefreshCayenneCaches() {
        this.refreshCayenneCaches = false;
        return this;
    }

    public CayenneTestDataManagerBuilder entity(Class<?> entityType) {
        return entities(entityType);
    }

    public CayenneTestDataManagerBuilder entities(Class<?>... entityTypes) {

        Objects.requireNonNull(entityTypes);
        for (Class<?> type : entityTypes) {
            entities.add(CayenneModelUtils.getDbEntity(resolver, type));
        }

        return this;
    }

    public CayenneTestDataManagerBuilder relatedTables(Class<?> entityType, Property<?> relationship) {
        CayenneModelUtils.getRelatedDbEntities(resolver, entityType, relationship).forEach(entities::add);
        return this;
    }

    public CayenneTestDataManagerBuilder tableName(String tableName) {
        return tableNames(tableName);
    }

    public CayenneTestDataManagerBuilder tableNames(String... tableNames) {

        Objects.requireNonNull(tableNames);
        for (String table : tableNames) {

            DbEntity entity = resolver.getDbEntity(table);
            if (entity == null) {
                throw new IllegalArgumentException("Not a Cayenne-managed table: " + table);
            }

            entities.add(entity);
        }

        return this;
    }
}
