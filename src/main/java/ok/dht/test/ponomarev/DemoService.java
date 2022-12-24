package ok.dht.test.ponomarev;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import ok.dht.Service;
import ok.dht.ServiceConfig;
import ok.dht.test.ServiceFactory;
import ok.dht.test.ponomarev.dao.MemorySegmentDao;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import one.nio.util.Utf8;

public class DemoService implements Service {
    private static final long limitBytes = 2024;

    private final ServiceConfig config;
    private HttpServer server;

    private MemorySegmentDao dao;

    public DemoService(ServiceConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<?> start() throws IOException {
        // Может создавать файлы
        dao = new MemorySegmentDao(config.workingDir(), limitBytes);

        server = new HttpServer(createConfigFromPort(config.selfPort()));
        // блокирующий старт (?)
        server.start();
        
        // Регистрируем текущий объект как хендлер запросов. 
        // Мб стоит вынести его в другой класс, а то тут каша какая-то уже
        server.addRequestHandlers(this);

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> stop() throws IOException {
        dao.close();
        server.stop();

        return CompletableFuture.completedFuture(null);
    }

    @Path("/")
    @RequestMethod(Request.METHOD_GET)
    public Response handleGet() {
        return new Response(
            Response.OK,
            Utf8.toBytes("Hello, world!\n")
        );
    }

    @ServiceFactory(stage = 0, week = 3)
    public static class Factory implements ServiceFactory.Factory {

        @Override
        public Service create(ServiceConfig config) {
            return new DemoService(config);
        }
    }

    private static HttpServerConfig createConfigFromPort(int port) {
        final AcceptorConfig acceptor = new AcceptorConfig();
        acceptor.port = port;
        acceptor.reusePort = true;

        final HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.acceptors = new AcceptorConfig[]{
            acceptor
        };

        return httpConfig;
    }
}
