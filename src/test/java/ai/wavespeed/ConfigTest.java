package ai.wavespeed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Config module (API configuration only, no serverless).
 */
class ConfigTest {

    @Test
    void testApiConfigHasExpectedAttributes() {
        // Test that API config has all expected attributes
        assertNotNull(Config.api);

        // Check all expected fields exist and have correct types
        assertTrue(Config.api.apiKey == null || Config.api.apiKey instanceof String);
        assertInstanceOf(String.class, Config.api.baseUrl);
        assertTrue(Config.api.connectionTimeout > 0);
        assertTrue(Config.api.timeout > 0);
        assertTrue(Config.api.maxRetries >= 0);
        assertTrue(Config.api.maxConnectionRetries >= 0);
        assertTrue(Config.api.retryInterval > 0);
    }

    @Test
    void testApiConfigDefaultValues() {
        // Test that API config has correct default values
        assertEquals("https://api.wavespeed.ai", Config.api.baseUrl);
        assertEquals(10.0, Config.api.connectionTimeout);
        assertEquals(36000.0, Config.api.timeout);
        assertEquals(0, Config.api.maxRetries);
        assertEquals(5, Config.api.maxConnectionRetries);
        assertEquals(1.0, Config.api.retryInterval);
    }

    @Test
    void testApiConfigIsModifiable() {
        // Test that API config fields can be modified
        String originalBaseUrl = Config.api.baseUrl;
        int originalMaxRetries = Config.api.maxRetries;

        try {
            Config.api.baseUrl = "https://custom.api.com";
            Config.api.maxRetries = 3;

            assertEquals("https://custom.api.com", Config.api.baseUrl);
            assertEquals(3, Config.api.maxRetries);
        } finally {
            // Restore original values
            Config.api.baseUrl = originalBaseUrl;
            Config.api.maxRetries = originalMaxRetries;
        }
    }

    @Test
    void testConfigSingleton() {
        // Test that Config.api is a singleton
        Config.Api api1 = Config.api;
        Config.Api api2 = Config.api;

        assertSame(api1, api2, "Config.api should be the same instance");
    }

    @Test
    void testConfigLoadedFromEnvironment() {
        // Test that config loads from environment variables
        // Note: Config is statically initialized, so we can't easily reset it
        String apiKey = System.getenv("WAVESPEED_API_KEY");

        if (apiKey != null && !apiKey.isEmpty()) {
            // Verify that Config.api.apiKey was set (even if setUp modified it later)
            assertNotNull(Config.api.apiKey, "Config.api.apiKey should be set when WAVESPEED_API_KEY exists");
        }
    }
}
