package ok.dht.test.ponomarev.rest.conf;

import java.util.Set;

import one.nio.http.Request;

public class ServerConfiguration {
    public static final String BASE_ENDPOINT = "/v0/entity";

    public static final Set<Integer> SUPPORTED_METHODS = Set.of(Request.METHOD_DELETE, Request.METHOD_GET, Request.METHOD_PUT);

    public static final long DAO_INMEMORY_LIMIT_BYTES = 2024;

    private ServerConfiguration() {}
}
