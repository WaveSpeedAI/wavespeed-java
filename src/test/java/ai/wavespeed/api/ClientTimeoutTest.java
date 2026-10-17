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
    private Double savedRead;
    private double sg, ss, su;

    @BeforeEach
    void save() {
        savedTimeout = Config.api.timeout;
        savedRead = Config.api.readTimeout; sg = Config.api.readTimeoutGet; ss = Config.api.readTimeoutSubmit; su = Config.api.readTimeoutUpload;
        Config.api.timeout = null;
        Config.api.readTimeout = null;
    }

    @AfterEach
    void restore() {
        Config.api.timeout = savedTimeout;
        Config.api.readTimeout = savedRead; Config.api.readTimeoutGet = sg; Config.api.readTimeoutSubmit = ss; Config.api.readTimeoutUpload = su;
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
        assertNull(new Config.Api().readTimeout);
        assertEquals(30.0, new Config.Api().readTimeoutGet);
        assertEquals(60.0, new Config.Api().readTimeoutSubmit);
        assertEquals(120.0, new Config.Api().readTimeoutUpload);
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
    void perTypeDefaults() {
        Client c = new Client("k");
        OkHttpClient get = c.requestClient(null, Client.Kind.GET);
        assertEquals(30_000, get.readTimeoutMillis());
        assertEquals(30_000, get.callTimeoutMillis());
        OkHttpClient submit = c.requestClient(null, Client.Kind.SUBMIT);
        assertEquals(60_000, submit.readTimeoutMillis());
        assertEquals(60_000, submit.callTimeoutMillis());
        OkHttpClient ticket = c.requestClient(null, Client.Kind.UPLOAD);
        assertEquals(120_000, ticket.readTimeoutMillis());
        OkHttpClient put = c.uploadPutClient(null);
        assertEquals(120_000, put.readTimeoutMillis());
        assertEquals(0, put.callTimeoutMillis());
        assertEquals(0, c.requestClient(null, Client.Kind.SYNC_SUBMIT).readTimeoutMillis());
    }

    @Test
    void overrideAppliesToGetSubmitUploadButNotSync() {
        Config.api.readTimeout = 5.0;
        Client c = new Client("k");
        for (Client.Kind k : new Client.Kind[]{Client.Kind.GET, Client.Kind.SUBMIT, Client.Kind.UPLOAD}) {
            assertEquals(5_000, c.requestClient(null, k).readTimeoutMillis(), k.name());
            assertEquals(5_000, c.requestClient(null, k).callTimeoutMillis(), k.name());
        }
        OkHttpClient put = c.uploadPutClient(null);
        assertEquals(5_000, put.readTimeoutMillis());
        assertEquals(0, put.callTimeoutMillis());
        assertEquals(0, c.requestClient(null, Client.Kind.SYNC_SUBMIT).readTimeoutMillis());
        assertEquals(0, c.requestClient(null, Client.Kind.SYNC_SUBMIT).callTimeoutMillis());
    }

    @Test
    void baseClientReadTimeoutIsTheGetDefaultNotInfinite() throws Exception {
        java.lang.reflect.Field f = Client.class.getDeclaredField("httpClient");
        f.setAccessible(true);
        assertEquals(30_000, ((OkHttpClient) f.get(new Client("k"))).readTimeoutMillis());
    }

    @Test
    void syncSubmitHasNoTimeoutByDefaultAndExplicitWhenGiven() {
        Client c = new Client("k");
        OkHttpClient none = c.requestClient(null, Client.Kind.SYNC_SUBMIT);
        assertEquals(0, none.readTimeoutMillis());
        assertEquals(0, none.callTimeoutMillis());
        OkHttpClient explicit = c.requestClient(7.0, Client.Kind.SYNC_SUBMIT);
        assertEquals(7_000, explicit.readTimeoutMillis());
        assertEquals(7_000, explicit.callTimeoutMillis());
    }

    @Test
    void explicitTimeoutKeepsPerRequestBehaviorForAsync() {
        OkHttpClient r = new Client("k").requestClient(7.0, Client.Kind.SUBMIT);
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

    @Test
    void uploadExplicitTimeoutKeepsTodaysBehavior() {
        OkHttpClient put = new Client("k").uploadPutClient(7.0);
        assertEquals(7_000, put.readTimeoutMillis());
        assertEquals(7_000, put.callTimeoutMillis());
    }

    /**
     * Raw PUT endpoint whose response head trickles in over ~1.6s, with a
     * piece every 400ms (each gap is inside a 0.6s read timeout). Total time exceeds the read timeout; inactivity never does.
     */
    private String uploadAgainstSlowPut(Double explicit, java.nio.file.Path dir) throws Exception {
        java.nio.file.Path f = dir.resolve("a.bin");
        java.nio.file.Files.write(f, new byte[]{1, 2, 3});
        try (java.net.ServerSocket raw = new java.net.ServerSocket(0);
             MockWebServer server = new MockWebServer()) {
            Thread t = new Thread(() -> {
                try (java.net.Socket sock = raw.accept()) {
                    java.io.InputStream in = sock.getInputStream();
                    byte[] buf = new byte[8192];
                    in.read(buf);  // request head + tiny body in one segment
                    java.io.OutputStream out = sock.getOutputStream();
                    sock.setTcpNoDelay(true);
                    for (String part : new String[]{"HTTP/1.1 200 OK\r\n", "Content-Length: 0\r\n",
                            "Connection: close\r\n", "\r\n"}) {
                        Thread.sleep(400);
                        out.write(part.getBytes());
                        out.flush();
                    }
                } catch (Exception ignored) {
                }
            });
            t.setDaemon(true);
            t.start();
            String putUrl = "http://127.0.0.1:" + raw.getLocalPort() + "/put";
            server.enqueue(new MockResponse().setBody("{\"code\":200,\"data\":{\"download_url\":\"https://e/x\","
                    + "\"upload\":{\"method\":\"PUT\",\"url\":\"" + putUrl + "\"}}}"));
            return new Client("k", server.url("/").toString(), null, 0, 0, 0.0).upload(f.toString(), explicit);
        }
    }

    @Test
    void throttledPutLongerThanReadTimeoutIsNotAborted(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        Config.api.readTimeout = 0.6;
        assertEquals("https://e/x", uploadAgainstSlowPut(null, dir));
    }

    @Test
    void explicitTimeoutStillIsATotalDeadlineForUpload(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        Config.api.readTimeout = 0.6;
        assertThrows(RuntimeException.class, () -> uploadAgainstSlowPut(0.6, dir));
    }
}
