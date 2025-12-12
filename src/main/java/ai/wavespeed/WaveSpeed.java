package ai.wavespeed;

import ai.wavespeed.openapi.client.ApiClient;
import ai.wavespeed.openapi.client.ApiException;
import ai.wavespeed.openapi.client.JSON;
import ai.wavespeed.openapi.client.api.DefaultApi;
import ai.wavespeed.openapi.client.model.Prediction;
import ai.wavespeed.openapi.client.model.PredictionResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Getter
@Slf4j
public class WaveSpeed extends DefaultApi {

    private final double pollIntervalSeconds;
    private final Double defaultTimeoutSeconds;
    private final String apiKey;
    private final int maxRetries;
    private final int maxConnectionRetries;
    private final double retryInterval;

    public WaveSpeed() {
        this(null, null, null, null, null, null);
    }

    public WaveSpeed(String apiKey) {
        this(apiKey, null, null, null, null, null);
    }

    public WaveSpeed(String apiKey, Double pollIntervalSeconds, Double timeoutSeconds) {
        this(apiKey, pollIntervalSeconds, timeoutSeconds, null, null, null);
    }

    public WaveSpeed(String apiKey, Double pollIntervalSeconds, Double timeoutSeconds,
                     Integer maxRetries, Integer maxConnectionRetries, Double retryInterval) {
        super();
        String resolvedKey = apiKey != null ? apiKey : System.getenv("WAVESPEED_API_KEY");
        if (resolvedKey == null || resolvedKey.isEmpty()) {
            throw new RuntimeException("Not set WAVESPEED_API_KEY environment variable or constructor apiKey.");
        }
        this.apiKey = resolvedKey;

        String envBase = System.getenv("WAVESPEED_BASE_URL");
        String baseUrl = envBase != null && !envBase.isEmpty() ? envBase : "https://api.wavespeed.ai";
        ApiClient client = getApiClient();
        client.setBasePath(baseUrl.replaceAll("/+$", "") + "/api/v3");
        client.setBearerToken(this.apiKey);

        Double envPoll = parseEnvDouble("WAVESPEED_POLL_INTERVAL");
        Double envTimeout = parseEnvDouble("WAVESPEED_TIMEOUT");
        this.pollIntervalSeconds = pollIntervalSeconds != null ? pollIntervalSeconds :
                (envPoll != null ? envPoll : 1.0);
        this.defaultTimeoutSeconds = timeoutSeconds != null ? timeoutSeconds :
                (envTimeout != null ? envTimeout : 36000.0);

        this.maxRetries = maxRetries != null ? maxRetries : 0;
        this.maxConnectionRetries = maxConnectionRetries != null ? maxConnectionRetries : 5;
        this.retryInterval = retryInterval != null ? retryInterval : 1.0;
    }

    private Double parseEnvDouble(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Prediction run(String modelId, Map<String, Object> input) throws ApiException {
        return this.run(modelId, input, null, null, null);
    }

    public Prediction run(String modelId, Map<String, Object> input, Double timeoutSeconds, Double pollIntervalSeconds)
            throws ApiException {
        return this.run(modelId, input, timeoutSeconds, pollIntervalSeconds, null);
    }

    public Prediction run(String modelId, Map<String, Object> input, Double timeoutSeconds, Double pollIntervalSeconds,
                          Boolean enableSyncMode) throws ApiException {
        boolean syncMode = enableSyncMode != null && enableSyncMode;
        ApiException lastError = null;

        for (int attempt = 0; attempt <= this.maxRetries; attempt++) {
            try {
                return runOnce(modelId, input, timeoutSeconds, pollIntervalSeconds, syncMode);
            } catch (ApiException e) {
                lastError = e;
                boolean isRetryable = isRetryableError(e);

                if (!isRetryable || attempt >= this.maxRetries) {
                    throw e;
                }

                long delay = (long) (this.retryInterval * (attempt + 1) * 1000);
                log.info("Task attempt {}/{} failed: {}", attempt + 1, this.maxRetries + 1, e.getMessage());
                log.info("Retrying in {}ms...", delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }

        throw lastError != null ? lastError : new ApiException("All " + (this.maxRetries + 1) + " attempts failed");
    }

    private Prediction runOnce(String modelId, Map<String, Object> input, Double timeoutSeconds,
                               Double pollIntervalSeconds, boolean enableSyncMode) throws ApiException {
        // Add enable_sync_mode to input if requested
        Map<String, Object> bodyData = input;
        if (enableSyncMode) {
            bodyData = new java.util.HashMap<>(input);
            bodyData.put("enable_sync_mode", true);
        }

        PredictionResponse predictionResponse = createPredictionDataWithRetry(modelId, bodyData, null);
        if (predictionResponse.getCode() != 200) {
            throw new ApiException(String.format("Failed : Response error code : %s, message: %s",
                    predictionResponse.getCode(), predictionResponse.getMessage()));
        }
        Prediction prediction = predictionResponse.getData();

        // In sync mode, the prediction is already complete
        if (enableSyncMode) {
            return prediction;
        }

        // Async mode: poll for completion
        double interval = pollIntervalSeconds != null ? pollIntervalSeconds : this.pollIntervalSeconds;
        Double waitTimeout = timeoutSeconds != null ? timeoutSeconds : this.defaultTimeoutSeconds;
        long start = System.currentTimeMillis();

        try {
            while (prediction.getStatus() != Prediction.StatusEnum.COMPLETED &&
                    prediction.getStatus() != Prediction.StatusEnum.FAILED) {
                Thread.sleep((long) (interval * 1000));
                if (waitTimeout != null) {
                    double elapsed = (System.currentTimeMillis() - start) / 1000.0;
                    if (elapsed >= waitTimeout) {
                        throw new ApiException(String.format("Prediction timed out after %.2f seconds", waitTimeout));
                    }
                }
                log.debug("Polling prediction: {} status: {}", prediction.getId(), prediction.getStatus());
                predictionResponse = getPredictionDataWithRetry(prediction.getId());
                prediction = predictionResponse.getData();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return prediction;
    }

    private boolean isRetryableError(ApiException e) {
        if (e == null) return false;
        String message = e.getMessage().toLowerCase();
        // Retry on timeout, connection errors, and 5xx/429 errors
        return message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("http 5") ||
                message.contains("429");
    }

    private PredictionResponse createPredictionDataWithRetry(String modelId, Map<String, Object> input, String webhook)
            throws ApiException {
        ApiException lastError = null;
        for (int retry = 0; retry <= this.maxConnectionRetries; retry++) {
            try {
                return createPredictionData(modelId, input, webhook);
            } catch (ApiException e) {
                lastError = e;
                if (retry < this.maxConnectionRetries) {
                    long delay = (long) (this.retryInterval * (retry + 1) * 1000);
                    log.info("Connection error on attempt {}/{}: {}", retry + 1, this.maxConnectionRetries + 1, e.getMessage());
                    log.info("Retrying in {}ms...", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw new ApiException("Failed to submit prediction after " + (this.maxConnectionRetries + 1) + " attempts: " + e.getMessage());
                }
            }
        }
        throw lastError;
    }

    private PredictionResponse getPredictionDataWithRetry(String predictionId) throws ApiException {
        ApiException lastError = null;
        for (int retry = 0; retry <= this.maxConnectionRetries; retry++) {
            try {
                return getPredictionData(predictionId);
            } catch (ApiException e) {
                lastError = e;
                if (retry < this.maxConnectionRetries) {
                    long delay = (long) (this.retryInterval * (retry + 1) * 1000);
                    log.info("Connection error getting result on attempt {}/{}: {}", retry + 1, this.maxConnectionRetries + 1, e.getMessage());
                    log.info("Retrying in {}ms...", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw new ApiException("Failed to get result after " + (this.maxConnectionRetries + 1) + " attempts: " + e.getMessage());
                }
            }
        }
        throw lastError;
    }

    public Prediction create(String id, Map<String, Object> input) throws ApiException {
        PredictionResponse predictionResponse = createPredictionData(id, input, null);
        if (predictionResponse.getCode() != 200) {
            throw new ApiException(String.format("Failed : Response error code : %s, message: %s",
                    predictionResponse.getCode(), predictionResponse.getMessage()));
        }
        return predictionResponse.getData();
    }

    public Prediction getPrediction(String predictionId) throws ApiException {
        PredictionResponse predictionResponse = getPredictionData(predictionId);
        if (predictionResponse.getCode() != 200) {
            throw new ApiException(String.format("Failed : Response error code : %s, message: %s",
                    predictionResponse.getCode(), predictionResponse.getMessage()));
        }
        return predictionResponse.getData();
    }

    public String upload(String filePath) throws ApiException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ApiException(String.format("File not found: %s", filePath));
        }

        OkHttpClient client = getApiClient().getHttpClient().newBuilder()
                .callTimeout(Duration.ofSeconds(120))
                .build();

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        String url = getApiClient().getBasePath().replaceAll("/+$", "") + "/media/upload/binary";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ApiException(String.format("Failed to upload file: HTTP %d %s",
                        response.code(), response.message()));
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ApiException("Upload failed: empty response body");
            }
            String raw = body.string();
            JSON json = getApiClient().getJSON();
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = json.deserialize(raw, Map.class);
            Object codeObj = parsed.get("code");
            if (!(codeObj instanceof Number) || ((Number) codeObj).intValue() != 200) {
                throw new ApiException(String.format("Upload failed: %s", raw));
            }
            Object dataObj = parsed.get("data");
            if (!(dataObj instanceof Map)) {
                throw new ApiException("Upload failed: data missing in response");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object urlObj = data.get("download_url");
            if (urlObj == null) {
                throw new ApiException("Upload failed: download_url missing in response");
            }
            return urlObj.toString();
        } catch (IOException e) {
            throw new ApiException("Upload failed: " + e.getMessage());
        }
    }
}
