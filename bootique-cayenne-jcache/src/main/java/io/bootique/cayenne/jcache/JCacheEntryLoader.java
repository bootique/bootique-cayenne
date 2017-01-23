package io.bootique.cayenne.jcache;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.cache.QueryCacheEntryFactory;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.List;

public class JCacheEntryLoader implements EntryProcessor<String, List, List> {

    private QueryCacheEntryFactory entryFactory;

    public JCacheEntryLoader(QueryCacheEntryFactory entryFactory) {
        this.entryFactory = entryFactory;
    }

    @Override
    public List process(MutableEntry<String, List> entry, Object... arguments) throws EntryProcessorException {
        if (!entry.exists()) {

            Object result = entryFactory.createObject();

            // sanity checking result type and value (hopefully Cayenne will provide type-safe cache API eventually)
            if (!(result instanceof List)) {
                if (result == null) {
                    throw new CayenneRuntimeException("Null object created: " + entry.getKey());
                } else {
                    throw new CayenneRuntimeException("Invalid query result, expected List, got "
                            + result.getClass().getName());
                }
            }

            entry.setValue((List<?>) result);
        }

        return entry.getValue();
    }
}
