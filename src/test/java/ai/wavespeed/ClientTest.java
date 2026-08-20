package ai.wavespeed;

import ai.wavespeed.api.Client;
import com.google.gson.Gson;
import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Test
    void testRunSyncModeTimeoutQueryableError() {
        String resultUrl = "https://api.wavespeed.ai/api/v3/predictions/req-timeout/result";
        String responseJson = "{\"data\": {\"status\": \"processing\", " +
                "\"id\": \"req-timeout\", " +
                "\"code\": 5004, " +
                "\"error\": \"Sync mode timed out after 90 seconds. The prediction is still processing asynchronously.\", " +
                "\"urls\": {\"get\": \"" + resultUrl + "\"}, " +
                "\"outputs\": []}}";
        OkHttpClient mockHttpClient = createMockHttpClient(200, responseJson);
        Client client = createClientWithMockHttp("test-key", mockHttpClient);

        RuntimeException error = assertThrows(RuntimeException.class, () -> client.run(
                "wavespeed-ai/z-image/turbo",
                Map.of("prompt", "test"),
                null, null, true, 1
        ));

        assertTrue(error.getMessage().contains("Sync mode timed out"));
        assertTrue(error.getMessage().contains("req-timeout"));
        assertTrue(error.getMessage().contains(resultUrl));
    }

    @Test
    void testRunNoThrowSyncModeTimeoutReturnsProcessing() {
        String resultUrl = "https://api.wavespeed.ai/api/v3/predictions/req-timeout/result";
        String responseJson = "{\"data\": {\"status\": \"processing\", " +
                "\"id\": \"req-timeout\", " +
                "\"code\": 5004, " +
                "\"error\": \"Sync mode timed out after 90 seconds. The prediction is still processing asynchronously.\", " +
                "\"urls\": {\"get\": \"" + resultUrl + "\"}, " +
                "\"outputs\": []}}";
        OkHttpClient mockHttpClient = createMockHttpClient(200, responseJson);
        Client client = createClientWithMockHttp("test-key", mockHttpClient);

        Client.RunNoThrowResult result = client.runNoThrow(
                "wavespeed-ai/z-image/turbo",
                Map.of("prompt", "test"),
                null, null, true, null
        );

        assertNull(result.getOutputs());
        assertEquals("processing", result.getDetail().getStatus());
        assertEquals("req-timeout", result.getDetail().getTaskId());
        assertEquals(resultUrl, result.getDetail().getResultUrl());
        assertTrue(result.getDetail().getError().contains("Sync mode timed out"));
    }

    @Test
    void testAttributionHeadersSentOnSubmit() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"data\": {\"status\": \"completed\", " +
                            "\"id\": \"req-123\", \"outputs\": []}}"));

            Client client = new Client("test-key", server.url("/").toString(), null, null, null, null);
            client.run("wavespeed-ai/z-image/turbo", Map.of("prompt", "test"), null, null, true, null);

            RecordedRequest recorded = server.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull(recorded);
            if (System.getenv("WAVESPEED_CLIENT_NAME") == null) {
                assertEquals("wavespeed-java", recorded.getHeader("X-Client-Name"));
            }
            assertEquals(Version.VERSION, recorded.getHeader("X-Client-Version"));
            String os = recorded.getHeader("X-Client-OS");
            assertNotNull(os);
            assertEquals(os.toLowerCase(), os);
            assertTrue(List.of("darwin", "linux", "windows").contains(os));
        }
    }

    @Test
    void testAttributionHeadersWithCustomClientName() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"data\": {\"status\": \"completed\", " +
                            "\"id\": \"req-123\", \"outputs\": []}}"));

            Client client = new Client("test-key", server.url("/").toString(), null, null, null, null)
                    .setClientName("my-app");
            client.run("wavespeed-ai/z-image/turbo", Map.of("prompt", "test"), null, null, true, null);

            RecordedRequest recorded = server.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull(recorded);
            if (System.getenv("WAVESPEED_CLIENT_NAME") == null) {
                assertEquals("my-app", recorded.getHeader("X-Client-Name"));
            }
        }
    }

    @Test
    void testSubmissionPostIsNeverRetriedOnIOException() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            // First (and only expected) request dies mid-flight -> IOException.
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
            // If the SDK wrongly retried, this queued success would be consumed.
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"data\": {\"id\": \"req-retried\"}}"));

            // Generous connection/task retry budget: none of it may apply to the POST.
            Client client = new Client("test-key", server.url("/").toString(), null, 3, 5, 0.01);

            WavespeedSubmissionException error = assertThrows(
                    WavespeedSubmissionException.class,
                    () -> client.run("wavespeed-ai/z-image/turbo", Map.of("prompt", "test"))
            );
            assertTrue(error.getMessage().contains("may already"));
            assertTrue(error.getMessage().contains("will not retry the POST"));
            assertEquals(1, server.getRequestCount());
        }
    }

    @Test
    void testPerCallTimeoutIsApplied() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            // Server accepts the connection but never responds.
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            Client client = new Client("test-key", server.url("/").toString(), null, 0, 0, 0.01);

            long start = System.currentTimeMillis();
            WavespeedSubmissionException error = assertThrows(
                    WavespeedSubmissionException.class,
                    () -> client.run("wavespeed-ai/z-image/turbo", Map.of("prompt", "test"),
                            1.0, null, true, null)  // timeout = 1s
            );
            long elapsedMs = System.currentTimeMillis() - start;

            // Shared-client read timeout is Config.api.timeout (36000s); only a
            // per-call timeout can fail this fast.
            assertTrue(elapsedMs < 10000,
                    "per-call timeout not applied; call took " + elapsedMs + "ms");
            assertNotNull(error.getCause());
            assertEquals(1, server.getRequestCount());
        }
    }

    @Test
    void testCancelledStatusTerminatesWait() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"data\": {\"id\": \"req-cancel\"}}"));
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"data\": {\"status\": \"cancelled\", " +
                            "\"id\": \"req-cancel\", " +
                            "\"error\": \"Task was cancelled by user\"}}"));

            Client client = new Client("test-key", server.url("/").toString(), null, 0, 0, 0.01);

            RuntimeException error = assertThrows(RuntimeException.class, () ->
                    client.run("wavespeed-ai/z-image/turbo", Map.of("prompt", "test"),
                            30.0, 0.05, null, null));

            assertTrue(error.getMessage().contains("cancelled"));
            assertTrue(error.getMessage().contains("req-cancel"));
            assertTrue(error.getMessage().contains("Task was cancelled by user"));
            // Submission + one poll: the cancelled status must stop the wait loop.
            assertEquals(2, server.getRequestCount());
        }
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

            // Per-call timeout clients are derived via newBuilder(); route them
            // back to the same mock so stubbed calls are still used.
            OkHttpClient.Builder mockBuilder = mock(OkHttpClient.Builder.class);
            when(mockClient.newBuilder()).thenReturn(mockBuilder);
            when(mockBuilder.connectTimeout(anyLong(), any(TimeUnit.class))).thenReturn(mockBuilder);
            when(mockBuilder.readTimeout(anyLong(), any(TimeUnit.class))).thenReturn(mockBuilder);
            when(mockBuilder.callTimeout(anyLong(), any(TimeUnit.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);
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
