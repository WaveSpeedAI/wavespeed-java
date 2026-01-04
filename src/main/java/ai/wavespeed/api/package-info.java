/**
 * WaveSpeed API client module.
 *
 * <p>Provides a simple interface to run WaveSpeed AI models.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * import ai.wavespeed.Wavespeed;
 *
 * Map<String, Object> output = Wavespeed.run(
 *     "wavespeed-ai/z-image/turbo",
 *     Map.of("prompt", "A beautiful sunset over mountains")
 * );
 *
 * // Get first output URL
 * @SuppressWarnings("unchecked")
 * List<String> outputs = (List<String>) output.get("outputs");
 * System.out.println(outputs.get(0));
 *
 * // Upload a file
 * String url = Wavespeed.upload("/path/to/image.png");
 * System.out.println(url);
 * }</pre>
 */
package ai.wavespeed.api;
