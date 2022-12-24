package ok.dht.test.ponomarev;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;

import ok.dht.Service;
import ok.dht.ServiceConfig;

public final class Server {
    static final String PORT_PROP_KEY = "port";

    static final Properties prop = new Properties();

    static {
        prop.setProperty(PORT_PROP_KEY, "8080");
    }

    private Server() {}

    public static void main(String[] args) throws IOException {
        final int port = Integer.parseInt(prop.getProperty(PORT_PROP_KEY));
        final String url = "http://localhost:" + port;
        
        final ServiceConfig cfg = new ServiceConfig(
            port,
            url,
            Collections.singletonList(url),
            Files.createTempDirectory("server")
        );

        final Service service = new DemoService(cfg);
        
    }
}
