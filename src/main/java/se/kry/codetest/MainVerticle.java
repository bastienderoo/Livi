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
    private static final String DELETE_QUERY = "DELETE FROM service WHERE url=?";
    private static final String UPDATE_STATUS_QUERY = "UPDATE service SET status = ? WHERE url = ?";
    private static final String REGEX_URL = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private HashMap<String, PollService> services = new HashMap<>();
    private DBConnector connector;
    private BackgroundPoller poller = new BackgroundPoller();

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        getAllServices();
        vertx.setPeriodic(200 * 60, timerId -> poller.pollServices(services).setHandler(this::checkChangeStatus));
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
                services = new HashMap<>();

                List<JsonArray> rows = resultSet.result().getResults();
                rows
                        .stream()
                        .map(row -> new PollService(
                                row.getString(0),
                                row.getString(1),
                                Status.valueOf(row.getString(3)),
                                getLocalDateFromLong(row.getLong(2))
                        ))
                        .forEach((PollService pollService) -> services.put(pollService.getUrl(), pollService));
                System.out.println("Get All services");
            }
            if (resultSet.failed()) {
                System.out.println("Failed to get Results");
            }
        });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        getRoute(router);
        postRoute(router);
        deleteRoute(router);
    }

    private void getRoute(Router router) {
        router.get("/service").handler(req -> {
            List<JsonObject> jsonServices = services
                    .entrySet()
                    .stream()
                    .map(service ->
                            new JsonObject()
                                    .put("url", service.getKey())
                                    .put("status", service.getValue().getStatus())
                                    .put("creationDate", service.getValue().getCreationDate().toString())
                                    .put("name", service.getValue().getName()))
                    .collect(Collectors.toList());
            req.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonArray(jsonServices).encode());
        });
    }

    private void postRoute(Router router) {
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            postService(new PollService(jsonBody.getString("name"), jsonBody.getString("url"), Status.NOT_TESTED, LocalDate.now()));
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
    }


    private void postService(PollService pollService) {
        if(pollService.getUrl().matches(REGEX_URL)){

        JsonArray params = new JsonArray()
                .add(pollService.getUrl())
                .add(pollService.getName())
                .add(pollService.getStatus().toString())
                .add(pollService.getCreationDate().toString());
        connector.query(POST_QUERY, params).setHandler((AsyncResult<ResultSet> asyncResultSet) -> {
            if (asyncResultSet.succeeded()) {
                getAllServices();
                System.out.println("Pushed successfully");
            }
            if (asyncResultSet.failed()) {
                System.out.println("An error as occurred");
            }
        });
        } else {
            System.out.println("Url doesn't match url pattern");
        }
    }


    private void deleteRoute(Router router) {
        router.delete("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            deleteService(jsonBody.getString("url"));
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
    }


    private void deleteService(String url) {
        JsonArray params = new JsonArray()
                .add(url);
        connector.query(DELETE_QUERY, params).setHandler((AsyncResult<ResultSet> asyncResultSet) -> {
            if (asyncResultSet.succeeded()) {
                getAllServices();
                System.out.println("Removed successfully");
            }
            if (asyncResultSet.failed()) {
                System.out.println("An error as occurred");
            }
        });
    }

    private void checkChangeStatus(AsyncResult<List<String>> res) {
        services.values()
                .stream()
                .filter(service -> service.getStatus().equals(Status.OK))
                .filter(service -> !res.result().contains(service.getUrl()))
                .forEach(service -> changeStatus(service, Status.OK));
        services.values()
                .stream()
                .filter(service -> service.getStatus().equals(Status.FAIL))
                .filter(service -> res.result().contains(service.getUrl()))
                .forEach(service -> changeStatus(service, Status.FAIL));
        updateStatusForUnknown(res);
    }

    private void updateStatusForUnknown(AsyncResult<List<String>> res) {
        List<PollService> pollWithoutStatus = services.values()
                .stream()
                .filter(service -> service.getStatus().equals(Status.NOT_TESTED))
                .collect(Collectors.toList());
        pollWithoutStatus.forEach(pollService -> {
            if (res.result().contains(pollService.getUrl())){
                changeStatus(pollService, Status.OK);
            } else {
                changeStatus(pollService, Status.FAIL);
            }
        });
    }

    private void changeStatus(PollService service, Status status) {
        System.out.println("Change : " + service.getUrl() + " to status : " + status);
        JsonArray params = new JsonArray()
                .add(status)
                .add(service.getUrl());
        connector.query(UPDATE_STATUS_QUERY, params).setHandler((AsyncResult<ResultSet> asyncResultSet) -> {
            if (asyncResultSet.succeeded()) {
                getAllServices();
                System.out.println("Update successfully");
            }
            if (asyncResultSet.failed()) {
                System.out.println("An error as occurred");
            }
        });
    }


    private LocalDate getLocalDateFromLong(long date) {
        return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}



