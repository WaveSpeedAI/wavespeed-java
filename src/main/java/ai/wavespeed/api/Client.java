package ai.wavespeed.api;

import ai.wavespeed.Config;
import ai.wavespeed.Version;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
    private String clientName;

    /**
     * Default value for the X-Client-Name channel-attribution header.
     */
    private static final String DEFAULT_CLIENT_NAME = "wavespeed-java";

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
     * Create a client with API key and max retries.
     *
     * @param apiKey WaveSpeed API key
     * @param maxRetries Maximum number of task-level retries
     */
    public Client(String apiKey, int maxRetries) {
        this(apiKey, null, null, maxRetries, null, null);
    }

    /**
     * Create a client with API key and retry configuration.
     *
     * @param apiKey WaveSpeed API key
     * @param maxRetries Maximum number of task-level retries
     * @param maxConnectionRetries Maximum retries for individual HTTP requests
     * @param retryInterval Base interval between retries in seconds
     */
    public Client(String apiKey, int maxRetries, int maxConnectionRetries, double retryInterval) {
        this(apiKey, null, null, maxRetries, maxConnectionRetries, retryInterval);
    }

    /**
     * Create a client using configuration from Config.api.
     */
    public Client() {
        this(null, null, null, null, null, null);
    }

    /**
     * Set the client name reported in the X-Client-Name header for channel attribution.
     *
     * <p>The WAVESPEED_CLIENT_NAME environment variable takes precedence over this value.</p>
     *
     * @param clientName Client name to report
     * @return This client, for chaining
     */
    public Client setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    /**
     * Resolve the value for the X-Client-Name header.
     *
     * <p>Precedence: WAVESPEED_CLIENT_NAME environment variable &gt; setClientName() &gt; default.</p>
     *
     * @return Client name for channel attribution
     */
    private String resolveClientName() {
        String envName = System.getenv("WAVESPEED_CLIENT_NAME");
        if (envName != null && !envName.isEmpty()) {
            return envName;
        }
        if (clientName != null && !clientName.isEmpty()) {
            return clientName;
        }
        return DEFAULT_CLIENT_NAME;
    }

    /**
     * Get the operating system name for the X-Client-OS header
     * (lowercase: darwin/linux/windows).
     *
     * @return Normalized operating system name
     */
    private static String clientOs() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac") || osName.contains("darwin")) {
            return "darwin";
        }
        if (osName.contains("win")) {
            return "windows";
        }
        if (osName.contains("nux") || osName.contains("nix")) {
            return "linux";
        }
        return osName;
    }

    /**
     * Add the channel-attribution headers (X-Client-Name, X-Client-Version,
     * X-Client-OS) sent on every API request.
     *
     * @param builder Request builder to add headers to
     * @return The same builder, for chaining
     */
    private Request.Builder addClientHeaders(Request.Builder builder) {
        return builder
                .addHeader("X-Client-Name", resolveClientName())
                .addHeader("X-Client-Version", Version.VERSION)
                .addHeader("X-Client-OS", clientOs());
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
        headers.put("X-Client-Name", resolveClientName());
        headers.put("X-Client-Version", Version.VERSION);
        headers.put("X-Client-OS", clientOs());
        return headers;
    }

    /**
     * Submit a prediction request.
     *
     * @param model Model identifier
     * @param input Input parameters
     * @param enableSyncMode If true, wait for result in a best-effort single request
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
                Request request = addClientHeaders(new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(
                                gson.toJson(body),
                                MediaType.parse("application/json")
                        ))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json"))
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
                Request request = addClientHeaders(new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Authorization", "Bearer " + apiKey))
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

    private String getResultUrl(Map<String, Object> data) {
        Object urlsObj = data.get("urls");
        if (!(urlsObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> urls = (Map<String, Object>) urlsObj;
        Object resultUrl = urls.get("get");
        return resultUrl instanceof String ? (String) resultUrl : null;
    }

    private int getResultCode(Map<String, Object> data) {
        Object code = data.get("code");
        if (code instanceof Number) {
            return ((Number) code).intValue();
        }
        return 0;
    }

    private boolean isSyncTimeoutData(Map<String, Object> data) {
        String status = (String) data.get("status");
        String error = (String) data.get("error");
        return getResultCode(data) == 5004 ||
                ("processing".equals(status) && error != null && error.contains("Sync mode timed out"));
    }

    private String syncModeErrorMessage(Map<String, Object> data) {
        String error = (String) data.get("error");
        if (error == null) {
            error = "Unknown error";
        }

        String requestId = (String) data.get("id");
        if (requestId == null) {
            requestId = "unknown";
        }

        if (isSyncTimeoutData(data)) {
            String message = "Sync mode timed out (task_id: " + requestId + "): " + error;
            String resultUrl = getResultUrl(data);
            if (resultUrl != null && !message.contains(resultUrl)) {
                message += " Query the result later at: " + resultUrl;
            }
            return message;
        }

        return "Prediction failed (task_id: " + requestId + "): " + error;
    }

    /**
     * Run a model and wait for the output.
     *
     * @param model Model identifier (e.g., "wavespeed-ai/flux-dev")
     * @param input Input parameters for the model
     * @param timeout Maximum time to wait for completion (null = no timeout)
     * @param pollInterval Interval between status checks in seconds (null = 1.0)
     * @param enableSyncMode If true, use synchronous mode (best-effort single request) (null = false)
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
                        throw new RuntimeException(syncModeErrorMessage(data));
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
     * Run a model with sync mode enabled.
     *
     * @param model Model identifier
     * @param input Input parameters
     * @param enableSyncMode If true, use synchronous mode (best-effort single request)
     * @return Map containing "outputs" array
     */
    public Map<String, Object> run(String model, Map<String, Object> input, boolean enableSyncMode) {
        return run(model, input, null, null, enableSyncMode, null);
    }

    /**
     * Run a model with custom timeout.
     *
     * @param model Model identifier
     * @param input Input parameters
     * @param timeout Maximum time to wait for completion
     * @return Map containing "outputs" array
     */
    public Map<String, Object> run(String model, Map<String, Object> input, double timeout) {
        return run(model, input, timeout, null, null, null);
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

        String contentType;
        try {
            contentType = Files.probeContentType(fileObj.toPath());
        } catch (IOException ignored) {
            contentType = null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("filename", fileObj.getName());
        payload.put("size", fileObj.length());
        if (contentType != null) {
            payload.put("content_type", contentType);
        }

        Request request = addClientHeaders(new Request.Builder()
                .url(this.baseUrl + "/api/v3/media/uploads")
                .post(RequestBody.create(gson.toJson(payload), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new RuntimeException(
                        "Failed to create upload: HTTP " + response.code() + ": " + errorBody
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
            @SuppressWarnings("unchecked")
            Map<String, Object> upload = (Map<String, Object>) data.get("upload");
            if (downloadUrl == null || upload == null || upload.get("url") == null) {
                throw new RuntimeException("Upload failed: no download_url in response");
            }

            String method = upload.get("method") instanceof String ? (String) upload.get("method") : "PUT";
            RequestBody fileBody = RequestBody.create(
                    fileObj,
                    MediaType.parse(contentType != null ? contentType : "application/octet-stream")
            );
            Request.Builder uploadRequest = new Request.Builder()
                    .url((String) upload.get("url"))
                    .method(method, fileBody);
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadHeaders = (Map<String, Object>) upload.get("headers");
            if (uploadHeaders != null) {
                uploadHeaders.forEach((key, value) -> uploadRequest.addHeader(key, String.valueOf(value)));
            }

            try (Response uploadResponse = httpClient.newCall(uploadRequest.build()).execute()) {
                if (!uploadResponse.isSuccessful()) {
                    String errorBody = uploadResponse.body() != null ? uploadResponse.body().string() : "";
                    throw new RuntimeException(
                            "Failed to upload file: HTTP " + uploadResponse.code() + ": " + errorBody
                    );
                }
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

    /**
     * Detail information for runNoThrow result.
     */
    public static class RunDetail {
        private final String taskId;
        private final String status;
        private final String model;
        private final String error;
        private final String createdAt;
        private final String resultUrl;

        public RunDetail(String taskId, String status, String model, String error, String createdAt) {
            this(taskId, status, model, error, createdAt, null);
        }

        public RunDetail(String taskId, String status, String model, String error, String createdAt, String resultUrl) {
            this.taskId = taskId;
            this.status = status;
            this.model = model;
            this.error = error;
            this.createdAt = createdAt;
            this.resultUrl = resultUrl;
        }

        public String getTaskId() { return taskId; }
        public String getStatus() { return status; }
        public String getModel() { return model; }
        public String getError() { return error; }
        public String getCreatedAt() { return createdAt; }
        public String getResultUrl() { return resultUrl; }
    }

    /**
     * Result for runNoThrow method.
     */
    public static class RunNoThrowResult {
        private final Object outputs;
        private final RunDetail detail;

        public RunNoThrowResult(Object outputs, RunDetail detail) {
            this.outputs = outputs;
            this.detail = detail;
        }

        public Object getOutputs() { return outputs; }
        public RunDetail getDetail() { return detail; }
    }

    /**
     * Run a model and wait for the output (no-throw version).
     *
     * <p>This method is similar to run() but does not throw exceptions for task failures.
     * Instead, it returns a result object with outputs (null on failure) and detail information.
     * The detail object always contains the taskId, which is useful for debugging and tracking.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * RunNoThrowResult result = client.runNoThrow(
     *     "wavespeed-ai/z-image/turbo",
     *     Map.of("prompt", "Cat")
     * );
     *
     * if (result.getOutputs() != null) {
     *     System.out.println("Success: " + result.getOutputs());
     *     System.out.println("Task ID: " + result.getDetail().getTaskId());
     * } else {
     *     System.out.println("Failed: " + result.getDetail().getError());
     *     System.out.println("Task ID: " + result.getDetail().getTaskId());
     * }
     * }</pre>
     *
     * @param model Model identifier
     * @param input Input parameters
     * @param timeout Maximum time to wait for completion (null = no timeout)
     * @param pollInterval Interval between status checks in seconds (null = 1.0)
     * @param enableSyncMode If true, use synchronous mode (best-effort single request) (null = false)
     * @param maxRetries Maximum task-level retries (null = use client setting)
     * @return RunNoThrowResult containing outputs and detail information
     */
    public RunNoThrowResult runNoThrow(
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

        for (int attempt = 0; attempt <= taskRetries; attempt++) {
            try {
                SubmitResult submitResult = submit(model, input, syncMode, timeout);

                if (syncMode) {
                    // In sync mode, extract outputs from the result
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) submitResult.syncResult.get("data");
                    String status = (String) data.get("status");
                    String taskId = (String) data.get("id");
                    if (taskId == null) taskId = "unknown";

                    if (!"completed".equals(status)) {
                        String error = (String) data.get("error");
                        if (error == null) error = "Unknown error";
                        String createdAt = (String) data.get("created_at");
                        String resultUrl = getResultUrl(data);
                        String detailStatus = "failed";
                        if (isSyncTimeoutData(data)) {
                            detailStatus = "processing";
                            error = syncModeErrorMessage(data);
                        }
                        
                        return new RunNoThrowResult(
                            null,
                            new RunDetail(taskId, detailStatus, model, error, createdAt, resultUrl)
                        );
                    }

                    String createdAt = (String) data.get("created_at");
                    return new RunNoThrowResult(
                        data.get("outputs"),
                        new RunDetail(taskId, "completed", model, null, createdAt)
                    );
                }

                // Async mode
                try {
                    Map<String, Object> result = wait(submitResult.requestId, timeout, poll);
                    return new RunNoThrowResult(
                        result.get("outputs"),
                        new RunDetail(submitResult.requestId, "completed", model, null, null)
                    );
                } catch (Exception waitError) {
                    // Wait failed, but we have taskID
                    return new RunNoThrowResult(
                        null,
                        new RunDetail(submitResult.requestId, "failed", model, waitError.getMessage(), null)
                    );
                }

            } catch (Exception e) {
                boolean isRetryable = isRetryableError(e);

                if (!isRetryable || attempt >= taskRetries) {
                    // Try to extract taskID from error message
                    String taskId = "unknown";
                    String errorMsg = e.getMessage();
                    if (errorMsg != null) {
                        int idx = errorMsg.indexOf("task_id: ");
                        if (idx != -1) {
                            int start = idx + 9;
                            int end = start;
                            while (end < errorMsg.length() && 
                                   errorMsg.charAt(end) != ')' && 
                                   errorMsg.charAt(end) != ' ' && 
                                   errorMsg.charAt(end) != '\n') {
                                end++;
                            }
                            if (end > start) {
                                taskId = errorMsg.substring(start, end);
                            }
                        }
                    }

                    return new RunNoThrowResult(
                        null,
                        new RunDetail(taskId, "failed", model, errorMsg, null)
                    );
                }

                System.out.println("Task attempt " + (attempt + 1) + "/" + (taskRetries + 1) + " failed: " + e);
                double delay = retryInterval * (attempt + 1);
                System.out.println("Retrying in " + delay + " seconds...");
                try {
                    Thread.sleep((long) (delay * 1000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new RunNoThrowResult(
                        null,
                        new RunDetail("unknown", "failed", model, "Interrupted during retry", null)
                    );
                }
            }
        }

        // Should not reach here
        return new RunNoThrowResult(
            null,
            new RunDetail("unknown", "failed", model, "All " + (taskRetries + 1) + " attempts failed", null)
        );
    }

    /**
     * Run a model with default options (no-throw version).
     *
     * @param model Model identifier
     * @param input Input parameters
     * @return RunNoThrowResult containing outputs and detail information
     */
    public RunNoThrowResult runNoThrow(String model, Map<String, Object> input) {
        return runNoThrow(model, input, null, null, null, null);
    }
}
