package ai.wavespeed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests that call the real WaveSpeed API.
 * These tests require WAVESPEED_API_KEY environment variable to be set.
 */
class RealApiTest {

    @BeforeAll
    static void checkApiKey() {
        String apiKey = System.getenv("WAVESPEED_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isEmpty(),
            "WAVESPEED_API_KEY environment variable not set");
    }

    @Test
    void testRunRealApi() {
        // Test a real API call to wavespeed-ai/z-image/turbo
        // Reset default client and restore API key from env var
        resetDefaultClient();
        Config.api.apiKey = System.getenv("WAVESPEED_API_KEY");

        Map<String, Object> output = Wavespeed.run(
            "wavespeed-ai/z-image/turbo",
            Map.of("prompt", "A simple red circle on white background")
        );

        assertNotNull(output);
        assertTrue(output.containsKey("outputs"));
        assertInstanceOf(List.class, output.get("outputs"));

        @SuppressWarnings("unchecked")
        List<String> outputs = (List<String>) output.get("outputs");
        assertTrue(outputs.size() > 0);

        // Output should be a URL
        String firstOutput = outputs.get(0);
        assertTrue(firstOutput.startsWith("http"),
            "Output should be a URL starting with 'http'");
    }

    @Test
    void testUploadRealApi(@TempDir Path tempDir) throws IOException {
        // Test a real file upload to WaveSpeed
        // Reset default client and restore API key from env var
        resetDefaultClient();
        Config.api.apiKey = System.getenv("WAVESPEED_API_KEY");

        // Create a minimal valid PNG file (1x1 red pixel)
        byte[] pngData = new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53,
            (byte)0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte)0x9C, 0x63, (byte)0xF8, (byte)0xCF, (byte)0xC0,
            0x00, 0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x05,
            (byte)0xFE, (byte)0xD4, 0x00, 0x00, 0x00, 0x00, 0x49,
            0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
        };

        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, pngData);

        String url = Wavespeed.upload(testFile.toString());

        assertNotNull(url);
        assertTrue(url.startsWith("http"),
            "Upload should return a URL starting with 'http'");
    }

    // Helper methods

    private void resetDefaultClient() {
        // Use reflection to reset the default client
        try {
            java.lang.reflect.Field field = Wavespeed.class.getDeclaredField("defaultClient");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // Ignore if field access fails
        }
    }
}
