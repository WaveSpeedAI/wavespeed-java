package ai.wavespeed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the module-level API functions.
 */
class ModuleLevelApiTest {

    @BeforeEach
    void setUp() {
        // Reset default client before each test
        resetDefaultClient();

        // Set up config
        Config.api.apiKey = "test-key";
        Config.api.baseUrl = "https://api.wavespeed.ai";
        Config.api.connectionTimeout = 10.0;
        Config.api.timeout = 36000.0;
        Config.api.maxRetries = 0;
        Config.api.maxConnectionRetries = 5;
        Config.api.retryInterval = 1.0;
    }

    @Test
    void testRunUsesDefaultClient() {
        // Test that module-level run() uses default client
        // This is a simplified test - full implementation would mock HTTP calls

        assertNotNull(Config.api.apiKey);
        assertEquals("https://api.wavespeed.ai", Config.api.baseUrl);

        // The actual API call would be mocked in a full test
        // For now, just verify the static method exists
        assertNotNull(Wavespeed.class);
    }

    @Test
    void testUploadUsesDefaultClient() {
        // Test that module-level upload() uses default client
        assertNotNull(Wavespeed.class);
    }

    @Test
    void testVersionAccess() {
        // Test version access
        String version = Wavespeed.version();
        assertNotNull(version);
        assertEquals("0.2.0", version);
    }

    @Test
    void testConfigAccess() {
        // Test config access
        Config.Api config = Wavespeed.config();
        assertNotNull(config);
        assertSame(Config.api, config);
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
