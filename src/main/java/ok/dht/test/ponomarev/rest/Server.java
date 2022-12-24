package ok.dht.test.ponomarev.rest;

import java.io.IOException;

import ok.dht.test.ponomarev.rest.conf.ServerConfiguration;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;

public final class Server extends HttpServer {
    public Server(HttpServerConfig config, Object... routers) throws IOException {
        super(config, routers);
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        // TODO: Мб нам не нужна тут итерация по списку? Будет интересно посмотреть ускорит ли что-то в async-profiler
        // JIT?

        if (!ServerConfiguration.SUPPORTED_METHODS.contains(request.getMethod())) {
            session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
        }

        if (!ServerConfiguration.SUPPORTED_ENDPOINTS.contains(request.getPath())) {
            session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
        }
    }
}
