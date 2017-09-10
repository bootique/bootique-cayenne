package io.bootique.cayenne.test;

import io.bootique.BQRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @since 0.24
 */
public class CayenneTestDataManagerBuilder {

    private BQRuntime runtime;
    private boolean deleteData;
    private boolean refreshCayenneCaches;
    private Set<DbEntity> dbEntities;
    private Set<DbEntity> dbEntityGraphRoots;

    private EntityResolver resolver;

    CayenneTestDataManagerBuilder(BQRuntime runtime) {
        this.runtime = runtime;
        this.deleteData = true;
        this.refreshCayenneCaches = true;

        ServerRuntime serverRuntime = runtime.getInstance(ServerRuntime.class);
        this.resolver = serverRuntime.getDataDomain().getEntityResolver();
        this.dbEntities = new HashSet<>();
        this.dbEntityGraphRoots = new HashSet<>();
    }

    public CayenneTestDataManager build() {

        Set<DbEntity> mergedEntities = mergeEntities();

        return new CayenneTestDataManager(
                runtime.getInstance(ServerRuntime.class).getDataDomain(),
                runtime.getInstance(CayenneTableManager.class),
                deleteData,
                refreshCayenneCaches,
                CayenneModelUtils.tablesInInsertOrder(runtime, mergedEntities));
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
            dbEntities.add(CayenneModelUtils.getDbEntity(resolver, type));
        }

        return this;
    }

    public CayenneTestDataManagerBuilder entitiesAndDependencies(Class<?>... entityTypes) {

        Objects.requireNonNull(entityTypes);
        for (Class<?> type : entityTypes) {
            dbEntityGraphRoots.add(CayenneModelUtils.getDbEntity(resolver, type));
        }

        return this;
    }

    public CayenneTestDataManagerBuilder relatedTables(Class<?> entityType, Property<?> relationship) {
        CayenneModelUtils.getRelatedDbEntities(resolver, entityType, relationship).forEach(dbEntities::add);
        return this;
    }

    public CayenneTestDataManagerBuilder table(String tableName) {
        return tables(tableName);
    }

    public CayenneTestDataManagerBuilder tables(String... tableNames) {

        Objects.requireNonNull(tableNames);
        for (String table : tableNames) {
            dbEntities.add(CayenneModelUtils.getDbEntity(resolver, table));
        }

        return this;
    }

    public CayenneTestDataManagerBuilder tablesAndDependencies(String... tableNames) {

        Objects.requireNonNull(tableNames);
        for (String table : tableNames) {
            dbEntityGraphRoots.add(CayenneModelUtils.getDbEntity(resolver, table));
        }

        return this;
    }

    private Set<DbEntity> mergeEntities() {
        if(dbEntityGraphRoots.isEmpty()) {
            return dbEntities;
        }

        Set<DbEntity> merged  = ModelDependencyResolver.resolve(dbEntityGraphRoots);
        merged.addAll(dbEntities);
        return merged;
    }

}
