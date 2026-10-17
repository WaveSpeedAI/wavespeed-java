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
         * Read and call timeout in seconds for a single async submit or
         * result-query request when no timeout is configured, so a hung
         * request fails on its own. Sync-mode submit is not subject to it.
         * Default: 120.0 (WAVESPEED_READ_TIMEOUT)
         */
        public double readTimeout;

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
            this.readTimeout = getEnvDouble("WAVESPEED_READ_TIMEOUT", 120.0);
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
