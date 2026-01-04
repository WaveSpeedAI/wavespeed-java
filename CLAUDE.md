# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WaveSpeed Java SDK - Official Java SDK for WaveSpeedAI inference platform. Provides an API client for running models.

## Commands

### Testing
```bash
# Run all tests
mvn test

# Run a single test file
mvn test -Dtest=ClientTest

# Run a specific test
mvn test -Dtest=ClientTest#testInitWithApiKey -X
```

### Building
```bash
# Build the project
mvn clean package

# Install to local Maven repository
mvn clean install

# Build without running tests
mvn clean package -DskipTests

# Compile only
mvn compile
```

## Architecture

### API Client (`src/main/java/ai/wavespeed/`)

Entry point: `Wavespeed.run(model, input)` or `new Client(apiKey)`

Three execution modes:
1. **Module-level API** - `Wavespeed.run()` for convenience
2. **Client instance** - `new Client()` for custom configuration
3. **Static import** - `import static ai.wavespeed.Wavespeed.*` for concise usage

Key classes:
- `Wavespeed.java` - Module-level API with static methods
- `api/Client.java` - Main client implementation
- `Config.java` - Global configuration (Config.api)
- `Version.java` - SDK version

### Client Structure

```java
// Module-level API (uses default client)
Map<String, Object> output = Wavespeed.run("model", input);
String url = Wavespeed.upload("/path/to/file");

// Client instance
Client client = new Client("api-key");
Map<String, Object> output = client.run("model", input);

// Access global config
Wavespeed.config().maxRetries = 3;
```

### Features

- **Sync Mode**: Single request that waits for result (`enableSyncMode`)
- **Retry Logic**: Configurable task-level and connection-level retries
- **Timeout Control**: Per-request and overall timeouts
- **File Upload**: Direct file upload to WaveSpeed storage

### Configuration

Global configuration via `Config.api`:
- `apiKey` - WaveSpeed API key
- `baseUrl` - API base URL (default: https://api.wavespeed.ai)
- `connectionTimeout` - Connection timeout in seconds (default: 10.0)
- `timeout` - Total API call timeout in seconds (default: 36000.0)
- `maxRetries` - Task-level retries (default: 0)
- `maxConnectionRetries` - HTTP connection retries (default: 5)
- `retryInterval` - Base retry delay in seconds (default: 1.0)

Client-level configuration via constructor:
```java
Client client = new Client(
    apiKey,
    baseUrl,
    connectionTimeout,
    maxRetries,
    maxConnectionRetries,
    retryInterval
);
```

### Environment Variables

- `WAVESPEED_API_KEY` - API key
- `WAVESPEED_BASE_URL` - Base URL (default: https://api.wavespeed.ai)
- `WAVESPEED_CONNECTION_TIMEOUT` - Connection timeout (default: 10.0)
- `WAVESPEED_TIMEOUT` - Total timeout (default: 36000.0)
- `WAVESPEED_MAX_RETRIES` - Task-level retries (default: 0)
- `WAVESPEED_MAX_CONNECTION_RETRIES` - HTTP retries (default: 5)
- `WAVESPEED_RETRY_INTERVAL` - Retry interval (default: 1.0)

## Project Structure

```
src/main/java/ai/wavespeed/
├── Wavespeed.java          # Module-level API
├── Version.java            # SDK version
├── Config.java             # Global configuration
└── api/
    ├── Client.java         # Main client implementation
    └── package-info.java   # Package documentation

src/test/java/ai/wavespeed/
└── (test files)

pom.xml                     # Maven configuration
```

## Dependencies

Key dependencies (see `pom.xml`):
- OkHttp 4.12.0 - HTTP client
- Gson 2.10.1 - JSON serialization
- JUnit 5 - Testing framework

## Release Process

This project uses Maven for versioning and GitHub Actions for releases. See VERSIONING.md for details.

To create a release:
1. Update version in `pom.xml`
2. Commit and tag: `git tag v0.1.0`
3. Push: `git push origin v0.1.0`
4. GitHub Actions will publish to Maven Central

## Code Style

This project uses:
- Standard Java naming conventions (camelCase for methods, PascalCase for classes)
- No Lombok (plain Java)
- System.out for logging (simple approach)
- Direct field access for Config.api (public fields)
