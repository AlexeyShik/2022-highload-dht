package ok.dht.test.ponomarev.rest.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import ok.dht.Service;
import ok.dht.ServiceConfig;
import ok.dht.test.ponomarev.dao.MemorySegmentDao;
import ok.dht.test.ponomarev.rest.Server;
import ok.dht.test.ponomarev.rest.conf.ServerConfiguration;
import ok.dht.test.ponomarev.rest.handlers.EntityRequestHandler;
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
        try {
            final Path workingDir = config.workingDir();
            if (Files.notExists(workingDir)) {
                Files.createDirectory(workingDir);
            }

            // Может создавать файлы
            dao = new MemorySegmentDao(workingDir, ServerConfiguration.DAO_INMEMORY_LIMIT_BYTES);

            server = new Server(createConfig(config.selfPort()));
            // Регистрируем текущий объект как хендлер запросов.
            server.addRequestHandlers(new EntityRequestHandler(dao));
            // блокирующий старт (?)
            server.start();

            return STUB_COMPLETED_FUTURE;
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<?> stop() throws IOException {
        dao.close();
        server.stop();

        return STUB_COMPLETED_FUTURE;
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
