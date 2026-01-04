package ai.wavespeed.api;

import ai.wavespeed.Config;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WaveSpeed API client.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Client client = new Client("your-api-key");
 * Map<String, Object> output = client.run(
 *     "wavespeed-ai/z-image/turbo",
 *     Map.of("prompt", "A cat")
 * );
 * System.out.println(output.get("outputs"));
 *
 * // With sync mode
 * output = client.run(
 *     "wavespeed-ai/z-image/turbo",
 *     Map.of("prompt", "A cat"),
 *     null,  // timeout
 *     null,  // pollInterval
 *     true,  // enableSyncMode
 *     null   // maxRetries
 * );
 *
 * // With retry
 * output = client.run(
 *     "wavespeed-ai/z-image/turbo",
 *     Map.of("prompt", "A cat"),
 *     null,  // timeout
 *     null,  // pollInterval
 *     null,  // enableSyncMode
 *     3      // maxRetries
 * );
 * }</pre>
 */
public class Client {
    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final int maxRetries;
    private final int maxConnectionRetries;
    private final double retryInterval;

    /**
     * Initialize the client.
     *
     * @param apiKey WaveSpeed API key. If null, uses Config.api.apiKey
     * @param baseUrl Base URL for the API. If null, uses Config.api.baseUrl
     * @param connectionTimeout Timeout for HTTP requests in seconds. If null, uses Config.api.connectionTimeout
     * @param maxRetries Maximum number of retries for the entire operation. If null, uses Config.api.maxRetries
     * @param maxConnectionRetries Maximum retries for individual HTTP requests. If null, uses Config.api.maxConnectionRetries
     * @param retryInterval Base interval between retries in seconds. If null, uses Config.api.retryInterval
     */
    public Client(
            String apiKey,
            String baseUrl,
            Double connectionTimeout,
            Integer maxRetries,
            Integer maxConnectionRetries,
            Double retryInterval
    ) {
        this.apiKey = apiKey != null ? apiKey : Config.api.apiKey;
        this.baseUrl = (baseUrl != null ? baseUrl : Config.api.baseUrl).replaceAll("/$", "");

        double connTimeout = connectionTimeout != null ? connectionTimeout : Config.api.connectionTimeout;
        double totalTimeout = Config.api.timeout;

        // Build HTTP client
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout((long) (connTimeout * 1000), TimeUnit.MILLISECONDS)
                .readTimeout((long) (totalTimeout * 1000), TimeUnit.MILLISECONDS)
                .build();

        this.gson = new Gson();
        this.maxRetries = maxRetries != null ? maxRetries : Config.api.maxRetries;
        this.maxConnectionRetries = maxConnectionRetries != null ? maxConnectionRetries : Config.api.maxConnectionRetries;
        this.retryInterval = retryInterval != null ? retryInterval : Config.api.retryInterval;
    }

    /**
     * Create a client with only API key.
     */
    public Client(String apiKey) {
        this(apiKey, null, null, null, null, null);
    }

    /**
     * Create a client using configuration from Config.api.
     */
    public Client() {
        this(null, null, null, null, null, null);
    }

    /**
     * Get request headers with authentication.
     *
     * @return Headers map
     * @throws IllegalArgumentException if API key is not configured
     */
    private Map<String, String> getHeaders() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                    "API key is required. Set WAVESPEED_API_KEY environment variable or pass api_key to Client()."
            );
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * Submit a prediction request.
     *
     * @param model Model identifier
     * @param input Input parameters
     * @param enableSyncMode If true, wait for result in single request
     * @param timeout Request timeout in seconds
     * @return Tuple of (request_id, result). In async mode, result is null. In sync mode, request_id is null.
     * @throws RuntimeException if submission fails after retries
     */
    private SubmitResult submit(
            String model,
            Map<String, Object> input,
            boolean enableSyncMode,
            Double timeout
    ) {
        // Validate API key early
        Map<String, String> headers = getHeaders();

        String url = this.baseUrl + "/api/v3/" + model;
        Map<String, Object> body = input != null ? new HashMap<>(input) : new HashMap<>();

        if (enableSyncMode) {
            body.put("enable_sync_mode", true);
        }

        double requestTimeout = timeout != null ? timeout : Config.api.timeout;
        double connectTimeout = Math.min(
                this.httpClient.connectTimeoutMillis() / 1000.0,
                requestTimeout
        );

        for (int retry = 0; retry <= maxConnectionRetries; retry++) {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(
                                gson.toJson(body),
                                MediaType.parse("application/json")
                        ))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.code() != 200) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new RuntimeException(
                                "Failed to submit prediction: HTTP " + response.code() + ": " + errorBody
                        );
                    }

                    String responseBody = response.body().string();
                    Map<String, Object> result = gson.fromJson(
                            responseBody,
                            new TypeToken<Map<String, Object>>() {}.getType()
                    );

                    if (enableSyncMode) {
                        return new SubmitResult(null, result);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    String requestId = (String) data.get("id");

                    if (requestId == null) {
                        throw new RuntimeException("No request ID in response: " + result);
                    }

                    return new SubmitResult(requestId, null);
                }

            } catch (IOException e) {
                System.out.println("Connection error on attempt " + (retry + 1) + "/" + (maxConnectionRetries + 1) + ":");
                e.printStackTrace();

                if (retry < maxConnectionRetries) {
                    double delay = retryInterval * (retry + 1);
                    System.out.println("Retrying in " + delay + " seconds...");
                    try {
                        Thread.sleep((long) (delay * 1000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                                "Failed to submit prediction after " + (maxConnectionRetries + 1) + " attempts",
                                e
                        );
                    }
                } else {
                    throw new RuntimeException(
                            "Failed to submit prediction after " + (maxConnectionRetries + 1) + " attempts",
                            e
                    );
                }
            }
        }

        throw new RuntimeException("Unexpected error in submit");
    }

    /**
     * Get prediction result.
     *
     * @param requestId The prediction request ID
     * @param timeout Request timeout in seconds
     * @return Full API response
     * @throws RuntimeException if fetching result fails after retries
     */
    private Map<String, Object> getResult(String requestId, Double timeout) {
        String url = this.baseUrl + "/api/v3/predictions/" + requestId + "/result";
        double requestTimeout = timeout != null ? timeout : Config.api.timeout;

        for (int retry = 0; retry <= maxConnectionRetries; retry++) {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.code() != 200) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new RuntimeException(
                                "Failed to get result for task " + requestId + ": HTTP " +
                                        response.code() + ": " + errorBody
                        );
                    }

                    String responseBody = response.body().string();
                    return gson.fromJson(
                            responseBody,
                            new TypeToken<Map<String, Object>>() {}.getType()
                    );
                }

            } catch (IOException e) {
                System.out.println("Connection error getting result on attempt " + (retry + 1) + "/" + (maxConnectionRetries + 1) + ":");
                e.printStackTrace();

                if (retry < maxConnectionRetries) {
                    double delay = retryInterval * (retry + 1);
                    System.out.println("Retrying in " + delay + " seconds...");
                    try {
                        Thread.sleep((long) (delay * 1000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                                "Failed to get result for task " + requestId + " after " +
                                        (maxConnectionRetries + 1) + " attempts",
                                e
                        );
                    }
                } else {
                    throw new RuntimeException(
                            "Failed to get result for task " + requestId + " after " +
                                    (maxConnectionRetries + 1) + " attempts",
                            e
                    );
                }
            }
        }

        throw new RuntimeException("Unexpected error in getResult");
    }

    /**
     * Wait for prediction to complete.
     *
     * @param requestId The prediction request ID
     * @param timeout Maximum wait time in seconds (null = no timeout)
     * @param pollInterval Time between polls in seconds
     * @return Map with "outputs" array
     * @throws RuntimeException if prediction fails
     * @throws RuntimeException if prediction times out
     */
    private Map<String, Object> wait(
            String requestId,
            Double timeout,
            double pollInterval
    ) {
        long startTime = System.currentTimeMillis();

        while (true) {
            // Check timeout
            if (timeout != null) {
                double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                if (elapsed >= timeout) {
                    throw new RuntimeException(
                            "Prediction timed out after " + timeout + " seconds (task_id: " + requestId + ")"
                    );
                }
            }

            Map<String, Object> result = getResult(requestId, timeout);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String status = (String) data.get("status");

            if ("completed".equals(status)) {
                Map<String, Object> output = new HashMap<>();
                output.put("outputs", data.get("outputs"));
                return output;
            }

            if ("failed".equals(status)) {
                String error = (String) data.get("error");
                throw new RuntimeException(
                        "Prediction failed (task_id: " + requestId + "): " +
                                (error != null ? error : "Unknown error")
                );
            }

            try {
                Thread.sleep((long) (pollInterval * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during polling", e);
            }
        }
    }

    /**
     * Determine if an error is worth retrying at the task level.
     *
     * @param error The exception to check
     * @return True if the error is retryable
     */
    private boolean isRetryableError(Exception error) {
        // Always retry timeout and connection errors
        if (error instanceof IOException) {
            return true;
        }

        // Retry server errors (5xx) and rate limiting (429)
        if (error instanceof RuntimeException) {
            String errorStr = error.getMessage();
            if (errorStr != null && (errorStr.contains("HTTP 5") || errorStr.contains("HTTP 429"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Run a model and wait for the output.
     *
     * @param model Model identifier (e.g., "wavespeed-ai/flux-dev")
     * @param input Input parameters for the model
     * @param timeout Maximum time to wait for completion (null = no timeout)
     * @param pollInterval Interval between status checks in seconds (null = 1.0)
     * @param enableSyncMode If true, use synchronous mode (single request) (null = false)
     * @param maxRetries Maximum task-level retries (null = use client setting)
     * @return Map containing "outputs" array with model outputs
     * @throws IllegalArgumentException if API key is not configured
     * @throws RuntimeException if the prediction fails
     * @throws RuntimeException if the prediction times out
     */
    public Map<String, Object> run(
            String model,
            Map<String, Object> input,
            Double timeout,
            Double pollInterval,
            Boolean enableSyncMode,
            Integer maxRetries
    ) {
        int taskRetries = maxRetries != null ? maxRetries : this.maxRetries;
        double poll = pollInterval != null ? pollInterval : 1.0;
        boolean syncMode = enableSyncMode != null && enableSyncMode;
        Exception lastError = null;

        for (int attempt = 0; attempt <= taskRetries; attempt++) {
            try {
                SubmitResult submitResult = submit(model, input, syncMode, timeout);

                if (syncMode) {
                    // In sync mode, extract outputs from the result
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) submitResult.syncResult.get("data");
                    String status = (String) data.get("status");

                    if (!"completed".equals(status)) {
                        String error = (String) data.get("error");
                        if (error == null) {
                            error = "Unknown error";
                        }
                        String requestId = (String) data.get("id");
                        if (requestId == null) {
                            requestId = "unknown";
                        }
                        throw new RuntimeException(
                                "Prediction failed (task_id: " + requestId + "): " + error
                        );
                    }

                    Map<String, Object> output = new HashMap<>();
                    output.put("outputs", data.get("outputs"));
                    return output;
                }

                return wait(submitResult.requestId, timeout, poll);

            } catch (Exception e) {
                lastError = e;
                boolean isRetryable = isRetryableError(e);

                if (!isRetryable || attempt >= taskRetries) {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }

                System.out.println("Task attempt " + (attempt + 1) + "/" + (taskRetries + 1) + " failed: " + e);
                double delay = retryInterval * (attempt + 1);
                System.out.println("Retrying in " + delay + " seconds...");
                try {
                    Thread.sleep((long) (delay * 1000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }

        // Should not reach here, but just in case
        if (lastError != null) {
            throw lastError instanceof RuntimeException ? (RuntimeException) lastError : new RuntimeException(lastError);
        }
        throw new RuntimeException("All " + (taskRetries + 1) + " attempts failed");
    }

    /**
     * Run a model with default options.
     *
     * @param model Model identifier
     * @param input Input parameters
     * @return Map containing "outputs" array
     */
    public Map<String, Object> run(String model, Map<String, Object> input) {
        return run(model, input, null, null, null, null);
    }

    /**
     * Upload a file to WaveSpeed.
     *
     * @param file File path string to upload
     * @param timeout Total API call timeout in seconds (null = use default)
     * @return URL of the uploaded file
     * @throws IllegalArgumentException if API key is not configured
     * @throws IllegalArgumentException if file path does not exist
     * @throws RuntimeException if upload fails
     */
    public String upload(String file, Double timeout) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                    "API key is required. Set WAVESPEED_API_KEY environment variable or pass api_key to Client()."
            );
        }

        File fileObj = new File(file);
        if (!fileObj.exists()) {
            throw new IllegalArgumentException("File not found: " + file);
        }

        String url = this.baseUrl + "/api/v3/media/upload/binary";
        RequestBody fileBody = RequestBody.create(
                fileObj,
                MediaType.parse("application/octet-stream")
        );

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileObj.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new RuntimeException(
                        "Failed to upload file: HTTP " + response.code() + ": " + errorBody
                );
            }

            String responseBody = response.body().string();
            Map<String, Object> result = gson.fromJson(
                    responseBody,
                    new TypeToken<Map<String, Object>>() {}.getType()
            );

            Double codeDouble = (Double) result.get("code");
            int code = codeDouble != null ? codeDouble.intValue() : 0;
            if (code != 200) {
                String message = (String) result.get("message");
                throw new RuntimeException("Upload failed: " + (message != null ? message : "Unknown error"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String downloadUrl = (String) data.get("download_url");
            if (downloadUrl == null) {
                throw new RuntimeException("Upload failed: no download_url in response");
            }

            return downloadUrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Upload a file with default timeout.
     *
     * @param file File path string to upload
     * @return URL of the uploaded file
     */
    public String upload(String file) {
        return upload(file, null);
    }

    /**
     * Internal class to hold submit result.
     */
    private static class SubmitResult {
        final String requestId;
        final Map<String, Object> syncResult;

        SubmitResult(String requestId, Map<String, Object> syncResult) {
            this.requestId = requestId;
            this.syncResult = syncResult;
        }
    }
}
