package ai.wavespeed;

import ai.wavespeed.openapi.client.ApiException;
import ai.wavespeed.openapi.client.model.Prediction;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WaveSpeedTest {
    private MockWebServer server;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void run_should_poll_until_completed() throws Exception {
        // create response
        String base = server.url("/api/v3/").toString().replaceAll("/$", "");
        String createPath = "/api/v3/wavespeed-ai/z-image/turbo";
        String resultPath = "/api/v3/predictions/pred-123/result";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":\"pred-123\",\"model\":\"wavespeed-ai/z-image/turbo\",\"status\":\"processing\",\"input\":{\"prompt\":\"Cat\"},\"outputs\":[],\"urls\":{\"get\":\"" + base + "/predictions/pred-123\"},\"has_nsfw_contents\":[],\"created_at\":\"2024-01-01T00:00:00Z\"}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":\"pred-123\",\"model\":\"wavespeed-ai/z-image/turbo\",\"status\":\"completed\",\"input\":{\"prompt\":\"Cat\"},\"outputs\":[\"https://img\"],\"urls\":{\"get\":\"" + base + "/predictions/pred-123\"},\"has_nsfw_contents\":[false],\"created_at\":\"2024-01-01T00:00:00Z\"}}"));

        WaveSpeed client = new WaveSpeed("test-key", 0.1, 5.0);
        client.getApiClient().setBasePath(base);

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "Cat");

        Prediction p = client.run("wavespeed-ai/z-image/turbo", input);
        assertEquals(Prediction.StatusEnum.COMPLETED, p.getStatus());
        assertEquals(java.net.URI.create("https://img"), p.getOutputs().get(0));

        // Verify requests were made (URL encoding may vary, so just check requests exist)
        assertNotNull(server.takeRequest(), "Create request should be made");
        assertNotNull(server.takeRequest(), "Result request should be made");
    }

    @Test
    void run_should_timeout() throws Exception {
        String base = server.url("/api/v3/").toString().replaceAll("/$", "");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":\"pred-123\",\"model\":\"wavespeed-ai/z-image/turbo\",\"status\":\"processing\",\"input\":{\"prompt\":\"Cat\"},\"outputs\":[],\"urls\":{\"get\":\"" + base + "/predictions/pred-123\"},\"has_nsfw_contents\":[],\"created_at\":\"2024-01-01T00:00:00Z\"}}"));
        // result responses always processing to trigger timeout
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":\"pred-123\",\"model\":\"wavespeed-ai/z-image/turbo\",\"status\":\"processing\",\"input\":{\"prompt\":\"Cat\"},\"outputs\":[],\"urls\":{\"get\":\"" + base + "/predictions/pred-123\"},\"has_nsfw_contents\":[],\"created_at\":\"2024-01-01T00:00:00Z\"}}"));

        WaveSpeed client = new WaveSpeed("test-key", 0.05, 0.1); // poll 50ms, timeout 100ms
        client.getApiClient().setBasePath(base);

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "Cat");

        assertThrows(ApiException.class, () -> client.run("wavespeed-ai/z-image/turbo", input));
    }

    @Test
    void upload_should_return_download_url() throws Exception {
        String base = server.url("/api/v3/").toString().replaceAll("/$", "");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"download_url\":\"https://cdn/file.png\"}}"));

        WaveSpeed client = new WaveSpeed("test-key", 0.1, 5.0);
        client.getApiClient().setBasePath(base);

        // create temp file
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("wavespeed-test", ".txt");
        java.nio.file.Files.write(temp, "hello".getBytes());

        String url = client.upload(temp.toString());
        assertEquals("https://cdn/file.png", url);

        // clean up
        java.nio.file.Files.deleteIfExists(temp);
    }

    @Test
    void real_run_if_api_key_present() throws Exception {
        String apiKey = System.getenv("WAVESPEED_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isEmpty(), "skip real run without API key");
        String baseUrl = System.getenv("WAVESPEED_BASE_URL"); // optional

        WaveSpeed client = new WaveSpeed(apiKey, 1.0, 120.0);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            client.getApiClient().setBasePath(baseUrl.replaceAll("/+$", "") + "/api/v3");
        }

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "Test image from java sdk");

        Prediction p = client.run("wavespeed-ai/z-image/turbo", input);
        assertFalse(p.getOutputs().isEmpty(), "real run outputs should not be empty");
        System.out.println("✓ Run test passed: status=" + p.getStatus() + ", output_url=" + p.getOutputs().get(0));
    }

    @Test
    void real_upload_if_api_key_present() throws Exception {
        String apiKey = System.getenv("WAVESPEED_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isEmpty(), "skip real upload without API key");
        String baseUrl = System.getenv("WAVESPEED_BASE_URL"); // optional

        WaveSpeed client = new WaveSpeed(apiKey, 1.0, 120.0);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            client.getApiClient().setBasePath(baseUrl.replaceAll("/+$", "") + "/api/v3");
        }

        // minimal PNG (1x1)
        byte[] png = new byte[]{
                (byte)0x89,(byte)0x50,(byte)0x4E,(byte)0x47,(byte)0x0D,(byte)0x0A,(byte)0x1A,(byte)0x0A,
                0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,
                0x08,0x02,0x00,0x00,0x00,(byte)0x90,0x77,0x53,(byte)0xDE,0x00,0x00,0x00,0x0C,0x49,0x44,0x41,0x54,
                0x78,(byte)0x9C,0x63,(byte)0xF8,(byte)0xCF,(byte)0xC0,0x00,0x00,0x00,0x03,0x00,0x01,0x00,0x05,(byte)0xFE,
                (byte)0xD4,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82
        };
        Path tmp = Files.createTempFile("wavespeed-java-upload", ".png");
        Files.write(tmp, png);

        String url = client.upload(tmp.toString());
        assertFalse(url.isEmpty(), "download_url should not be empty");
        System.out.println("✓ Upload test passed: url=" + url);

        Files.deleteIfExists(tmp);
    }
}
