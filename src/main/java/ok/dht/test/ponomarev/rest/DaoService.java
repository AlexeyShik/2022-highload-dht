package ok.dht.test.ponomarev.rest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import ok.dht.Service;
import ok.dht.ServiceConfig;
import ok.dht.test.ServiceFactory;
import ok.dht.test.ponomarev.dao.MemorySegmentDao;
import ok.dht.test.ponomarev.rest.conf.ServerConfiguration;
import ok.dht.test.ponomarev.rest.handlers.DaoRequestHandler;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.server.AcceptorConfig;

public class DaoService implements Service {
    private static final CompletableFuture<?> STUB_COMPLETED_FUTURE = CompletableFuture.completedFuture(null);

    private final ServiceConfig config;

    private HttpServer server;
    private MemorySegmentDao dao;

    public DaoService(ServiceConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<?> start() throws IOException {
        // Может создавать файлы
        server = new Server(createConfig(config.selfPort()));
        dao = new MemorySegmentDao(config.workingDir(), ServerConfiguration.DAO_INMEMORY_LIMIT_BYTES);

        // Регистрируем текущий объект как хендлер запросов. 
        // Мб стоит вынести его в другой класс, а то тут каша какая-то уже
        server.addRequestHandlers(new DaoRequestHandler(dao));

        // блокирующий старт (?)
        server.start();

        return STUB_COMPLETED_FUTURE;
    }

    @Override
    public CompletableFuture<?> stop() throws IOException {
        server.stop();
        dao.close();

        return STUB_COMPLETED_FUTURE;
    }


    @ServiceFactory(stage = 1, week = 3)
    public static class Factory implements ServiceFactory.Factory {

        @Override
        public Service create(ServiceConfig config) {
            return new DaoService(config);
        }
    }

    private static HttpServerConfig createConfig(int port) {
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
