package ai.wavespeed.api;

import ai.wavespeed.Config;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** Timeout semantics: no default overall cap; bounded per-request defaults. */
class ClientTimeoutTest {
    private Double savedTimeout;
    private double savedRead;

    @BeforeEach
    void save() {
        savedTimeout = Config.api.timeout;
        savedRead = Config.api.readTimeout;
        Config.api.timeout = null;
        Config.api.readTimeout = 120.0;
    }

    @AfterEach
    void restore() {
        Config.api.timeout = savedTimeout;
        Config.api.readTimeout = savedRead;
    }

    private static MockResponse submitOk() {
        return new MockResponse().setBody("{\"data\":{\"id\":\"t1\",\"status\":\"created\"}}");
    }

    private static MockResponse status(String s) {
        String out = "completed".equals(s) ? ",\"outputs\":[\"u\"]" : "";
        return new MockResponse().setBody("{\"data\":{\"id\":\"t1\",\"status\":\"" + s + "\"" + out + "}}");
    }

    private Client client(MockWebServer server) {
        return new Client("k", server.url("/").toString(), null, 0, 0, 0.0);
    }

    @Test
    void defaultConfigHasNoOverallCap() {
        assertNull(new ai.wavespeed.Config.Api().timeout);  // fresh default (env unset)
        assertEquals(120.0, new Config.Api().readTimeout);
    }

    @Test
    void waitKeepsPollingPastOldTenHourCapByDefault() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(submitOk());
            for (int i = 0; i < 5; i++) server.enqueue(status("processing"));
            server.enqueue(status("completed"));
            Client c = client(server);
            AtomicLong now = new AtomicLong();
            // every clock read jumps 100000s (> 36000s)
            c.nowMillis = () -> now.addAndGet(100_000_000L);
            Map<String, Object> out = c.run("m/x", Map.of("p", 1), null, 0.001, null, null);
            assertNotNull(out.get("outputs"));
            assertEquals(7, server.getRequestCount());
        }
    }

    @Test
    void explicitTimeoutStillBoundsOverallWait() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(submitOk());
            for (int i = 0; i < 5; i++) server.enqueue(status("processing"));
            Client c = client(server);
            AtomicLong now = new AtomicLong();
            c.nowMillis = () -> now.addAndGet(10_000L);
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> c.run("m/x", Map.of("p", 1), 25.0, 0.001, null, null));
            assertTrue(e.getMessage().contains("timed out after 25.0 seconds"), e.getMessage());
        }
    }

    @Test
    void configTimeoutAlsoBoundsOverallWait() throws Exception {
        Config.api.timeout = 25.0;
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(submitOk());
            for (int i = 0; i < 5; i++) server.enqueue(status("processing"));
            Client c = client(server);
            AtomicLong now = new AtomicLong();
            c.nowMillis = () -> now.addAndGet(10_000L);
            assertThrows(RuntimeException.class, () -> c.run("m/x", Map.of("p", 1), null, 0.001, null, null));
        }
    }

    @Test
    void asyncSubmitAndResultGetUseReadTimeoutDefault() {
        Client c = new Client("k");
        OkHttpClient r = c.requestClient(null, true);
        assertEquals(120_000, r.readTimeoutMillis());
        assertEquals(120_000, r.callTimeoutMillis());
        assertEquals(120_000, c.requestClient(null, true).callTimeoutMillis());
        // base client read timeout is the bounded default, not infinite
        Config.api.readTimeout = 30.0;
        assertEquals(30_000, c.requestClient(null, true).readTimeoutMillis());
    }

    @Test
    void syncSubmitHasNoTimeoutByDefaultAndExplicitWhenGiven() {
        Client c = new Client("k");
        OkHttpClient none = c.requestClient(null, false);
        assertEquals(0, none.readTimeoutMillis());
        assertEquals(0, none.callTimeoutMillis());
        OkHttpClient explicit = c.requestClient(7.0, false);
        assertEquals(7_000, explicit.readTimeoutMillis());
        assertEquals(7_000, explicit.callTimeoutMillis());
    }

    @Test
    void explicitTimeoutKeepsPerRequestBehaviorForAsync() {
        OkHttpClient r = new Client("k").requestClient(7.0, true);
        assertEquals(7_000, r.readTimeoutMillis());
        assertEquals(7_000, r.callTimeoutMillis());
        assertEquals(7_000, r.connectTimeoutMillis());  // min(10s, 7s)
    }

    @Test
    void hungResultGetFailsAloneAndIsRetried() throws Exception {
        Config.api.readTimeout = 0.3;
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(submitOk());
            server.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE));
            server.enqueue(status("completed"));
            Client c = new Client("k", server.url("/").toString(), null, 0, 2, 0.01);
            Map<String, Object> out = c.run("m/x", Map.of("p", 1), null, 0.001, null, null);
            assertNotNull(out.get("outputs"));
            assertEquals(3, server.getRequestCount());
        }
    }
}
