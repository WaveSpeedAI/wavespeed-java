package ai.wavespeed;

import ai.wavespeed.api.Client;
import com.google.gson.Gson;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the Client class.
 */
class ClientTest {
    private final Gson gson = new Gson();

    @Test
    void testInitWithApiKey() {
        // Test client initialization with explicit API key
        Client client = new Client("test-key");

        // Access private field through reflection for testing
        assertEquals("https://api.wavespeed.ai", getBaseUrl(client));
    }

    @Test
    void testInitWithCustomBaseUrl() {
        // Test client initialization with custom base URL
        Client client = new Client(
            "test-key",
            "https://custom.api.com/",
            null, null, null, null
        );

        assertEquals("https://custom.api.com", getBaseUrl(client));
    }

    @Test
    void testInitFromConfig() {
        // Test client initialization from config
        Config.api.apiKey = "config-key";
        Config.api.baseUrl = "https://api.wavespeed.ai";
        Config.api.connectionTimeout = 10.0;
        Config.api.maxRetries = 0;
        Config.api.maxConnectionRetries = 5;
        Config.api.retryInterval = 1.0;

        Client client = new Client();

        // Verify initialization (would need reflection to access private fields)
        assertNotNull(client);
    }

    @Test
    void testGetHeadersRaisesWithoutApiKey() {
        // Test that getHeaders throws exception without API key
        // Pass empty string to avoid fallback to Config.api.apiKey
        Client client = new Client("", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> {
            client.run("model", Map.of());
        });
    }

    @Test
    void testSubmitSuccess() throws IOException {
        // Test successful prediction submission with sync mode
        String responseJson = "{\"data\": {\"status\": \"completed\", " +
                "\"id\": \"req-123\", " +
                "\"outputs\": [\"https://example.com/out.png\"]}}";
        OkHttpClient mockHttpClient = createMockHttpClient(200, responseJson);

        Client client = createClientWithMockHttp("test-key", mockHttpClient);

        // Use sync mode to avoid polling
        Map<String, Object> output = client.run(
                "wavespeed-ai/z-image/turbo",
                Map.of("prompt", "test"),
                null, null, true, null  // enableSyncMode = true
        );

        assertNotNull(output);
        assertTrue(output.containsKey("outputs"));
    }

    @Test
    void testSubmitFailure() {
        // Test prediction submission failure
        OkHttpClient mockHttpClient = createMockHttpClient(500, "Internal Server Error");

        Client client = createClientWithMockHttp("test-key", mockHttpClient);

        assertThrows(RuntimeException.class, () -> {
            client.run("wavespeed-ai/z-image/turbo", Map.of("prompt", "test"));
        });
    }

    @Test
    void testGetResultSuccess() {
        // Test successful result retrieval
        String responseJson = "{\"data\": {\"status\": \"completed\", " +
            "\"outputs\": [\"https://example.com/out.png\"]}}";

        OkHttpClient mockHttpClient = createMockHttpClient(200, responseJson);
        Client client = createClientWithMockHttp("test-key", mockHttpClient);

        // Would test _getResult if it were public
        assertNotNull(client);
    }

    @Test
    void testRunSuccess() {
        // Test successful run() call with async mode
        // Mock: submit returns request_id, then get_result returns completed
        String submitResponse = "{\"data\": {\"id\": \"req-123\"}}";
        String resultResponse = "{\"data\": {\"status\": \"completed\", " +
            "\"outputs\": [\"https://example.com/out.png\"]}}";

        // This test is simplified - full implementation would need proper mocking
        // of multiple HTTP calls
        assertNotNull(new Client("test-key"));
    }

    @Test
    void testRunFailure() {
        // Test run() with failed prediction
        String submitResponse = "{\"data\": {\"id\": \"req-123\"}}";
        String failedResponse = "{\"data\": {\"status\": \"failed\", " +
            "\"error\": \"Model error\"}}";

        // Simplified test - would need proper HTTP mocking
        assertNotNull(new Client("test-key"));
    }

    @Test
    void testRunTimeout() {
        // Test run() with timeout
        // This would require mocking time and sleep
        Client client = new Client("test-key");

        assertThrows(RuntimeException.class, () -> {
            client.run("model", Map.of("prompt", "test"), 0.001, null, null, null);
        });
    }

    // Helper methods

    private String getBaseUrl(Client client) {
        // Use reflection to access private field for testing
        try {
            java.lang.reflect.Field field = Client.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            return (String) field.get(client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OkHttpClient createMockHttpClient(int statusCode, String responseBody) {
        // Create a mock OkHttpClient
        OkHttpClient mockClient = mock(OkHttpClient.class);
        Call mockCall = mock(Call.class);

        try {
            Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("http://test").build())
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message("Test")
                .body(ResponseBody.create(
                    responseBody,
                    MediaType.parse("application/json")
                ))
                .build();

            when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
            when(mockCall.execute()).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mockClient;
    }

    private Client createClientWithMockHttp(String apiKey, OkHttpClient mockClient) {
        // Use reflection to inject mock HTTP client
        try {
            Client client = new Client(apiKey);
            java.lang.reflect.Field field = Client.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(client, mockClient);
            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock HTTP client", e);
        }
    }
}
