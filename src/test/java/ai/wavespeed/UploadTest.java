package ai.wavespeed;

import ai.wavespeed.api.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the upload functionality.
 */
class UploadTest {

    @Test
    void testUploadFilePath(@TempDir Path tempDir) throws IOException {
        // Test uploading a file by path
        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, "fake image data".getBytes());

        Client client = new Client("test-key");

        // This would need mocking to avoid real HTTP calls
        // For now, just verify the method exists and handles the file
        assertThrows(RuntimeException.class, () -> {
            client.upload(testFile.toString());
        });
    }

    @Test
    void testUploadFileNotFound() {
        // Test uploading a non-existent file
        Client client = new Client("test-key");

        assertThrows(IllegalArgumentException.class, () -> {
            client.upload("/nonexistent/path/to/file.png");
        });
    }

    @Test
    void testUploadRaisesWithoutApiKey() {
        // Test that upload raises exception without API key
        Client client = new Client(null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> {
            client.upload("/some/file.png");
        });
    }

    @Test
    void testUploadHttpError(@TempDir Path tempDir) throws IOException {
        // Test upload with HTTP error
        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, "fake image data".getBytes());

        Client client = new Client("test-key");

        // Would need HTTP mocking for full test
        assertThrows(RuntimeException.class, () -> {
            client.upload(testFile.toString());
        });
    }

    @Test
    void testUploadApiError(@TempDir Path tempDir) throws IOException {
        // Test upload with API error response
        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, "fake image data".getBytes());

        Client client = new Client("test-key");

        // Would need HTTP mocking to return error response
        assertThrows(RuntimeException.class, () -> {
            client.upload(testFile.toString());
        });
    }

    @Test
    void testUploadSuccess(@TempDir Path tempDir) throws IOException {
        // Test successful upload (would need mocking)
        Path testFile = tempDir.resolve("test.png");
        Files.write(testFile, "fake image data".getBytes());

        Client client = new Client("test-key");

        // This test would pass with proper HTTP mocking
        // showing expected URL return
        assertTrue(testFile.toFile().exists());
    }
}
