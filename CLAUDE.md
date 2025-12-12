# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WaveSpeed Java SDK - Official Java SDK for WaveSpeedAI inference platform. Built with Maven and uses OpenAPI-generated client code.

## Commands

### Testing
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=WaveSpeedTest

# Run tests with verbose output
mvn test -X
```

### Building
```bash
# Build the project
mvn clean package

# Install to local Maven repository
mvn clean install

# Build without running tests
mvn clean package -DskipTests
```

### Development
```bash
# Compile only
mvn compile

# Clean build artifacts
mvn clean

# Generate documentation
mvn javadoc:javadoc
```

## Architecture

### Client Structure

Entry point: `new WaveSpeed(apiKey, pollInterval, timeout, maxRetries, maxConnectionRetries, retryInterval)`

The SDK provides a simple client for running models:

```java
WaveSpeed client = new WaveSpeed("your-api-key");
Prediction result = client.run("model-id", input);
```

### Key Classes

Located in `src/main/java/ai/wavespeed/`:

- `WaveSpeed` - Main client class (extends `DefaultApi`)
- `Main` - Example usage

OpenAPI-generated code in `src/main/java/ai/wavespeed/openapi/client/`:
- `ApiClient` - HTTP client wrapper
- `DefaultApi` - Base API class
- `model/Prediction` - Prediction model
- `model/PredictionResponse` - API response wrapper

### Features

- **Sync Mode**: Single request that waits for result (`enableSyncMode`)
- **Retry Logic**: Configurable task-level and connection-level retries
- **Timeout Control**: Per-request and overall timeouts
- **File Upload**: Direct file upload to WaveSpeed storage
- **Lombok Integration**: Uses `@Getter` and `@Slf4j` annotations

### Configuration

Client-level configuration via constructor:
- `apiKey` - WaveSpeed API key
- `pollIntervalSeconds` - Polling interval
- `timeoutSeconds` - Overall timeout
- `maxRetries` - Task-level retries (default: 0)
- `maxConnectionRetries` - HTTP connection retries (default: 5)
- `retryInterval` - Base retry delay in seconds (default: 1.0)

Per-request configuration:
- `run(modelId, input, timeout, pollInterval, enableSyncMode)`

### Environment Variables

- `WAVESPEED_API_KEY` - API key (required if not passed to constructor)
- `WAVESPEED_BASE_URL` - Base URL (default: https://api.wavespeed.ai)
- `WAVESPEED_POLL_INTERVAL` - Poll interval in seconds
- `WAVESPEED_TIMEOUT` - Timeout in seconds

## Project Structure

```
src/main/java/ai/wavespeed/
├── WaveSpeed.java              # Main SDK class
├── Main.java                   # Example usage
└── openapi/client/             # OpenAPI-generated code
    ├── ApiClient.java
    ├── api/DefaultApi.java
    └── model/
        ├── Prediction.java
        └── PredictionResponse.java

pom.xml                         # Maven configuration
```

## Dependencies

Key dependencies (see `pom.xml`):
- OkHttp - HTTP client
- Gson - JSON serialization
- Lombok - Code generation
- JUnit - Testing

## Testing

Tests are located in `src/test/java/`:
- Unit tests for WaveSpeed client
- Integration tests for API calls
- Test utilities and fixtures

## Release Process

This project uses Maven for versioning and GitHub Actions for releases. See VERSIONING.md for details.

To create a release:
1. Update version in `pom.xml`
2. Commit and tag: `git tag v1.0.0`
3. Push: `git push origin v1.0.0`
4. GitHub Actions will publish to Maven Central

## Code Style

This project uses:
- Lombok annotations for boilerplate reduction
- SLF4J for logging
- Builder pattern where appropriate
- Java naming conventions (camelCase for methods, PascalCase for classes)

## Logging

The SDK uses SLF4J with Lombok's `@Slf4j` annotation. Log levels:
- `DEBUG` - Polling status and detailed operations
- `INFO` - Retry attempts and important events
- `WARN` - Warnings and recoverable errors
- `ERROR` - Errors (not used extensively, exceptions are thrown)
