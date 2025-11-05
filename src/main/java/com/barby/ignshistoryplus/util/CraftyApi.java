package com.barby.ignshistoryplus.util;

import com.barby.ignshistoryplus.IgnsHistoryPlus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public final class CraftyApi {
    private static final String WORKER_BASE =
            "https://whoareyou-proxy.oculus-hnaif.workers.dev/v1/players/";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private CraftyApi() {}

    public static String getPlayerJson(String username) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WORKER_BASE + username))
                    .header("User-Agent", "IGNSHistoryPlus-Mod/1.0")
                    .GET()
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                IgnsHistoryPlus.LOGGER.warn("Worker returned HTTP {}", resp.statusCode());
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            IgnsHistoryPlus.LOGGER.error("Failed calling Worker", e);
            return null;
        }
    }
}
