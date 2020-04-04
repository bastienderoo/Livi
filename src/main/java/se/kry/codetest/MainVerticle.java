package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private static final String GET_ALL_QUERY = "SELECT * FROM service";
    private static final String POST_QUERY = "INSERT INTO service (url, name, status, creationDate) VALUES (?,?,?,?)";

    private HashMap<String, String> services = new HashMap<>();
    private DBConnector connector;
    private BackgroundPoller poller = new BackgroundPoller();

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        getAllServices();
        services.put("https://www.kry.se", "UNKNOWN");
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
        setRoutes(router);
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void getAllServices() {
        connector.query(GET_ALL_QUERY).setHandler((AsyncResult<ResultSet> resultSet) -> {
            if (resultSet.succeeded()) {

                List<JsonArray> rows = resultSet.result().getResults();
                rows
                        .stream()
                        .map(row -> new PollService(
                                row.getString(0),
                                row.getString(1),
                                Status.valueOf(row.getString(2)),
                                getLocalDateFromLong(row.getLong(3))
                        ))
                        .forEach((PollService pollService) -> services.put(pollService.getName(), pollService.getStatus().toString()));
                System.out.println("Get All services");
            }
            if (resultSet.failed()) {
                System.out.println("Failed to get Results");
            }
        });
    }

    private void postService(PollService pollService) {
        JsonArray params = new JsonArray()
                .add(pollService.getName())
                .add(pollService.getUrl())
                .add(pollService.getStatus().toString())
                .add(pollService.getCreationDate().toString());
        connector.query(POST_QUERY, params).setHandler((AsyncResult<ResultSet> asyncResultSet) -> {
            if (asyncResultSet.succeeded()) {
                System.out.println("Pushed successfully");
            }
            if (asyncResultSet.failed()) {
                System.out.println("An error as occurred");
            }
        });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        getService(router);
        postService(router);
    }

    private void getService(Router router) {
        router.get("/service").handler(req -> {
            List<JsonObject> jsonServices = services
                    .entrySet()
                    .stream()
                    .map(service ->
                            new JsonObject()
                                    .put("name", service.getKey())
                                    .put("status", service.getValue()))
                    .collect(Collectors.toList());
            req.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonArray(jsonServices).encode());
        });
    }

    private void postService(Router router) {
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            postService(new PollService("test", "test", Status.FAIL, LocalDate.now()));
            // TODO connect to DB to post
            services.put(jsonBody.getString("url"), "UNKNOWN");
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
    }


    private LocalDate getLocalDateFromLong(long date) {
        return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}



