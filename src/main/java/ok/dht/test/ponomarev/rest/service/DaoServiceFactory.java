package ok.dht.test.ponomarev.rest.service;

import ok.dht.Service;
import ok.dht.ServiceConfig;
import ok.dht.test.ServiceFactory;

@ServiceFactory(stage = 1, week = 3)
public class DaoServiceFactory implements ServiceFactory.Factory {

    @Override
    public Service create(ServiceConfig config) {
        return new DaoService(config);
    }
}