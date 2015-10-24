package org.sriki.githistory.api;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AbstractService {
    public static class Pair<K, V> {
        private final ArrayList<K> keys;
        private final ArrayList<V> values;

        public Pair(Collection<K> keys, Collection<V> values) {
            this.keys = new ArrayList<>(keys);
            this.values = new ArrayList<>(values);
        }

        public List<K> getKeys() {
            return keys;
        }

        public List<V> getValues() {
            return values;
        }
    }

    protected  <K, V> Response toResponse(Map<K, V> map) {
        return Response.ok()
                .entity(map)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS").build();
    }

    protected  <E> Response toResponse(Collection<E> data) {
        return Response.ok()
                .entity(data)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS").build();
    }


    protected Response toResponse(int[][] dayTime) {
        return Response.ok()
                .entity(dayTime)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS").build();
    }

}
