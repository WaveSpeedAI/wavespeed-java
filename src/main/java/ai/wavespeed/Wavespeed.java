package ai.wavespeed;

import ai.wavespeed.api.Client;

import java.util.Map;

/**
 * WaveSpeedAI Java Client — Official Java SDK for WaveSpeedAI inference platform.
 *
 * <p>This library provides a clean, unified, and high-performance API for your applications.
 * Effortlessly connect to all WaveSpeedAI models and inference services with zero infrastructure overhead.</p>
 *
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * import ai.wavespeed.Wavespeed;
 *
 * Map<String, Object> output = Wavespeed.run(
 *     "wavespeed-ai/z-image/turbo",
 *     Map.of("prompt", "A beautiful sunset")
 * );
 * System.out.println(output.get("outputs"));
 * }</pre>
 */
public final class Wavespeed {

    /**
     * Default client instance (lazy-initialized).
     */
    private static volatile Client defaultClient = null;

    /**
     * Private constructor to prevent instantiation.
     */
    private Wavespeed() {
        throw new AssertionError("Wavespeed class cannot be instantiated");
    }

    /**
     * Get or create the default client instance.
     *
     * @return The default Client instance
     */
    private static Client getDefaultClient() {
        if (defaultClient == null) {
            synchronized (Wavespeed.class) {
                if (defaultClient == null) {
                    defaultClient = new Client();
                }
            }
        }
        return defaultClient;
    }

    /**
     * Run a model and wait for the output.
     *
     * @param model Model identifier (e.g., "wavespeed-ai/flux-dev")
     * @param input Input parameters for the model
     * @param timeout Maximum time to wait for completion (null = no timeout)
     * @param pollInterval Interval between status checks in seconds (null = 1.0)
     * @param enableSyncMode If true, use synchronous mode (single request) (null = false)
     * @param maxRetries Maximum retries for this request (null = use default setting)
     * @return Map containing "outputs" array with model outputs
     * @throws IllegalArgumentException if API key is not configured
     * @throws RuntimeException if the prediction fails
     * @throws RuntimeException if the prediction times out
     *
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> output = Wavespeed.run(
     *     "wavespeed-ai/z-image/turbo",
     *     Map.of("prompt", "A cat sitting on a windowsill"),
     *     null, null, null, null
     * );
     * System.out.println(output.get("outputs"));  // First output URL
     *
     * // With sync mode
     * output = Wavespeed.run(
     *     "wavespeed-ai/z-image/turbo",
     *     Map.of("prompt", "A cat"),
     *     null, null, true, null
     * );
     *
     * // With retry
     * output = Wavespeed.run(
     *     "wavespeed-ai/z-image/turbo",
     *     Map.of("prompt", "A cat"),
     *     null, null, null, 3
     * );
     * }</pre>
     */
    public static Map<String, Object> run(
            String model,
            Map<String, Object> input,
            Double timeout,
            Double pollInterval,
            Boolean enableSyncMode,
            Integer maxRetries
    ) {
        return getDefaultClient().run(model, input, timeout, pollInterval, enableSyncMode, maxRetries);
    }

    /**
     * Run a model with default options.
     *
     * @param model Model identifier
     * @param input Input parameters
     * @return Map containing "outputs" array
     */
    public static Map<String, Object> run(String model, Map<String, Object> input) {
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * String url = Wavespeed.upload("/path/to/image.png", null);
     * System.out.println(url);
     * }</pre>
     */
    public static String upload(String file, Double timeout) {
        return getDefaultClient().upload(file, timeout);
    }

    /**
     * Upload a file with default timeout.
     *
     * @param file File path string to upload
     * @return URL of the uploaded file
     */
    public static String upload(String file) {
        return upload(file, null);
    }

    /**
     * Get access to the global API configuration.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Wavespeed.config().maxRetries = 3;
     * Wavespeed.config().baseUrl = "https://custom.api.com";
     * }</pre>
     *
     * @return The global Config.Api instance
     */
    public static Config.Api config() {
        return Config.api;
    }

    /**
     * Get the SDK version.
     *
     * @return Version string
     */
    public static String version() {
        return Version.VERSION;
    }
}
