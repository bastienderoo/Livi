package se.kry.codetest;

import io.vertx.core.Future;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackgroundPoller {

    List<String> successUrl = new ArrayList<>();

    public Future<List<String>> pollServices(Map<String, PollService> services) {
        services.values().forEach(service -> {
            getStatus(service.getUrl());
        });
        return Future.succeededFuture(successUrl);
    }

    public Status getStatus(String url) {
        Status status = Status.NOT_TESTED;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            // Set connection timeout
            con.setConnectTimeout(3000);
            con.connect();

            int code = con.getResponseCode();
            if (code == 200) {
                successUrl.add(url);
                status = Status.OK;
            }
        } catch (Exception e) {
            status = Status.FAIL;
        }
        return status;
    }
}
