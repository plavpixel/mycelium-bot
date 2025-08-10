package com.myceliumbot;

import okhttp3.*;

import java.io.IOException;

public class HttpUtils {
    private int timeoutSeconds;
    private boolean allowUnsafeConnections;

    // Default constructor - uses config
    public HttpUtils() {
        BotConfig config = BotConfig.getInstance();
        this.timeoutSeconds = config.getHttpTimeoutSeconds();
        this.allowUnsafeConnections = config.isAllowUnsafeConnections();
    }

    private final OkHttpClient client = new OkHttpClient();

    public String get(String url) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return "Error: " + response.code();
            return response.body() != null ? response.body().string() : "Error: Empty response body";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String post(String url, String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return "Error: " + response.code();
            return response.body() != null ? response.body().string() : "Error: Empty response body";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
}
