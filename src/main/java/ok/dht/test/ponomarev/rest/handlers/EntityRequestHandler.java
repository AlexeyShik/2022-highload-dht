package ok.dht.test.ponomarev.rest.handlers;

import java.io.IOException;

import jdk.incubator.foreign.MemorySegment;
import ok.dht.test.ponomarev.dao.MemorySegmentDao;
import ok.dht.test.ponomarev.dao.TimestampEntry;
import ok.dht.test.ponomarev.dao.Utils;
import ok.dht.test.ponomarev.rest.conf.ServerConfiguration;
import one.nio.http.Param;
import one.nio.http.Path;
import one.nio.http.Request;
import one.nio.http.RequestMethod;
import one.nio.http.Response;
import one.nio.util.Utf8;

public class EntityRequestHandler {
    private final MemorySegmentDao dao;

    private static final Response BAD_REQUEST = new Response(
        Response.BAD_REQUEST,
        Response.EMPTY
    );

    public EntityRequestHandler(MemorySegmentDao dao) {
        this.dao = dao;
    }

    @Path(ServerConfiguration.V_0_ENTITY_ENDPOINT)
    @RequestMethod(Request.METHOD_GET)
    public Response getById(@Param(value = "id", required = true) String id) {
        if (id.isEmpty()) {
            return BAD_REQUEST;
        }

        try {
            final TimestampEntry entry = dao.get(
                Utils.memorySegmentFromString(id)
            );

            if (entry == null) {
                return new Response(
                    Response.NOT_FOUND,

                    //TODO: Нужно ли нам здесь гонять лишние данные по сети?
                    Utf8.toBytes("Entry not found by id: " + id)
                );
            }

            return new Response(
                Response.OK,
                //TODO: Аллокация массива байт)))
                entry.value().toByteArray()
            );
        } catch (Exception e) {
            return new Response(
                Response.SERVICE_UNAVAILABLE,
                Response.EMPTY
            );
        }
    }

    // Это PUT и мы знаем ID, то так же его можно использовать для создания 
    // записи. Почему клиенту известен ID при создании, он должен аллоцироваться на сервере.
    @Path(ServerConfiguration.V_0_ENTITY_ENDPOINT)
    @RequestMethod(Request.METHOD_PUT)
    public Response put(Request request, @Param(value = "id", required = true) String id) {
        if (id.isEmpty()) {
            return BAD_REQUEST;
        }

        try {
            final byte[] body = request.getBody();
            if (body == null) {
                return new Response(
                    Response.BAD_REQUEST,
                    Response.EMPTY
                );
            }

            dao.upsert(
                new TimestampEntry(
                    Utils.memorySegmentFromString(id),
                    MemorySegment.ofArray(body),
                    System.currentTimeMillis()
                )
            );

            return new Response(
                Response.CREATED,
                Response.EMPTY
            );
        } catch (Exception e) {
            return new Response(
                Response.SERVICE_UNAVAILABLE,
                Response.EMPTY
            );
        }
    }

    @Path(ServerConfiguration.V_0_ENTITY_ENDPOINT)
    @RequestMethod(Request.METHOD_DELETE)
    public Response delete(@Param(value = "id", required = true) String id) {
        if (id.isEmpty()) {
            return BAD_REQUEST;
        }

        try {
            dao.upsert(
                new TimestampEntry(
                    Utils.memorySegmentFromString(id),
                    null,
                    System.currentTimeMillis()
                )
            );

            return new Response(
                Response.ACCEPTED,
                Response.EMPTY
            );
        } catch (Exception e) {
            return new Response(
                Response.SERVICE_UNAVAILABLE,
                Response.EMPTY
            );
        }
    }
}
