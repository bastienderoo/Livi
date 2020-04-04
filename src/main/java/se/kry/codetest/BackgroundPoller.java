package se.kry.codetest;

import io.vertx.core.Future;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackgroundPoller {

  public Future<List<String>> pollServices(Map<String, PollService> services) {
    // TODO implement poller
    return Future.failedFuture("TODO");
  }
}
