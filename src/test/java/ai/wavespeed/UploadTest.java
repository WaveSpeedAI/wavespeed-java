package ai.wavespeed;

import ai.wavespeed.api.Client;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UploadTest {
    @Test
    void testDirectUpload(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, "fake image data".getBytes());

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String uploadUrl = server.url("/storage-upload").toString();
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"code\":200,\"data\":{\"download_url\":\"https://example.com/file.png\","
                            + "\"upload\":{\"method\":\"PUT\",\"url\":\"" + uploadUrl
                            + "\",\"headers\":{\"Content-Type\":\"image/png\"}}}}"));
            server.enqueue(new MockResponse().setResponseCode(200));

            Client client = new Client("test-key", server.url("/").toString(), null, null, null, null);
            assertEquals("https://example.com/file.png", client.upload(testFile.toString()));

            RecordedRequest ticket = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(ticket);
            assertEquals("/api/v3/media/uploads", ticket.getPath());
            assertEquals("Bearer test-key", ticket.getHeader("Authorization"));
            assertTrue(ticket.getBody().readUtf8().contains("\"size\":15"));

            RecordedRequest upload = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(upload);
            assertEquals("PUT", upload.getMethod());
            assertNull(upload.getHeader("Authorization"));
            assertEquals("fake image data", upload.getBody().readUtf8());
        }
    }

    @Test
    void testUploadFileNotFound() {
        Client client = new Client("test-key");
        assertThrows(IllegalArgumentException.class, () -> client.upload("/nonexistent/path/to/file.png"));
    }

    @Test
    void testUploadRequiresApiKey(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, new byte[] {1});
        Client client = new Client("", null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> client.upload(testFile.toString()));
    }
}
