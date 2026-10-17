package ai.wavespeed;

/**
 * Configuration module for WaveSpeed SDK.
 *
 * Provides API client configuration through a static nested class.
 *
 * Example usage:
 * <pre>{@code
 * // Access global configuration
 * String apiKey = Config.api.apiKey;
 *
 * // Modify global configuration
 * Config.api.apiKey = "your-key";
 * Config.api.maxRetries = 3;
 * }</pre>
 */
public final class Config {

    /**
     * API client configuration options.
     *
     * All fields are public for direct access.
     */
    public static class Api {
        /**
         * WaveSpeed API key.
         * Loaded from WAVESPEED_API_KEY environment variable if not set.
         */
        public String apiKey;

        /**
         * API base URL.
         * Default: "https://api.wavespeed.ai"
         */
        public String baseUrl;

        /**
         * Connection timeout in seconds.
         * Default: 10.0
         */
        public double connectionTimeout;

        /**
         * Overall time limit in seconds for waiting on a task.
         * Default: null (no limit; the SDK waits until the task reaches a
         * terminal status). Set it, or WAVESPEED_TIMEOUT, to bound the wait.
         * A configured value also bounds each individual HTTP request.
         */
        public Double timeout;

        /**
         * Optional single override for the per-request read timeout in seconds.
         * When set (or WAVESPEED_READ_TIMEOUT), it replaces the per-type defaults
         * below for result/GET queries, async submit and uploads. It never
         * applies to sync-mode submit. Default: null.
         */
        public Double readTimeout;

        /**
         * Read and call timeout in seconds of one result query or other
         * read-only GET when no timeout is configured.
         * Default: 30.0 (WAVESPEED_READ_TIMEOUT_GET)
         */
        public double readTimeoutGet;

        /**
         * Read and call timeout in seconds of the async submit POST when no
         * timeout is configured. A submit timeout is an ambiguous outcome, so it
         * gets more headroom. Default: 60.0 (WAVESPEED_READ_TIMEOUT_SUBMIT)
         */
        public double readTimeoutSubmit;

        /**
         * Read timeout in seconds for each upload request (ticket and file PUT)
         * when no timeout is configured. The file PUT has no total-transfer
         * deadline. Default: 120.0 (WAVESPEED_READ_TIMEOUT_UPLOAD)
         */
        public double readTimeoutUpload;

        /**
         * Maximum number of retries for the entire operation (task-level retries).
         * Default: 0
         */
        public int maxRetries;

        /**
         * Maximum number of retries for individual HTTP requests (connection errors, timeouts).
         * Default: 5
         */
        public int maxConnectionRetries;

        /**
         * Base interval between retries in seconds.
         * Actual delay = retryInterval * attempt (exponential backoff).
         * Default: 1.0
         */
        public double retryInterval;

        /**
         * Initialize with default values from environment variables.
         */
        public Api() {
            this.apiKey = System.getenv("WAVESPEED_API_KEY");
            this.baseUrl = getEnv("WAVESPEED_BASE_URL", "https://api.wavespeed.ai");
            this.connectionTimeout = getEnvDouble("WAVESPEED_CONNECTION_TIMEOUT", 10.0);
            this.timeout = getEnvDoubleOrNull("WAVESPEED_TIMEOUT");
            this.readTimeout = getEnvDoubleOrNull("WAVESPEED_READ_TIMEOUT");
            this.readTimeoutGet = getEnvDouble("WAVESPEED_READ_TIMEOUT_GET", 30.0);
            this.readTimeoutSubmit = getEnvDouble("WAVESPEED_READ_TIMEOUT_SUBMIT", 60.0);
            this.readTimeoutUpload = getEnvDouble("WAVESPEED_READ_TIMEOUT_UPLOAD", 120.0);
            this.maxRetries = getEnvInt("WAVESPEED_MAX_RETRIES", 0);
            this.maxConnectionRetries = getEnvInt("WAVESPEED_MAX_CONNECTION_RETRIES", 5);
            this.retryInterval = getEnvDouble("WAVESPEED_RETRY_INTERVAL", 1.0);
        }

        private static String getEnv(String key, String defaultValue) {
            String value = System.getenv(key);
            return value != null ? value : defaultValue;
        }

        private static double getEnvDouble(String key, double defaultValue) {
            String value = System.getenv(key);
            if (value != null) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        private static Double getEnvDoubleOrNull(String key) {
            String value = System.getenv(key);
            if (value != null) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        private static int getEnvInt(String key, int defaultValue) {
            String value = System.getenv(key);
            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }
    }

    /**
     * Global API configuration instance.
     *
     * Use this to access or modify global configuration settings.
     */
    public static final Api api = new Api();

    private Config() {
        // Prevent instantiation
    }
}
