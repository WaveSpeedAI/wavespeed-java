<div align="center">
  <a href="https://wavespeed.ai" target="_blank" rel="noopener noreferrer">
    <img src="https://raw.githubusercontent.com/WaveSpeedAI/waverless/main/docs/images/wavespeed-dark-logo.png" alt="WaveSpeedAI logo" width="200"/>
  </a>

  <h1>WaveSpeedAI Java SDK</h1>

  <p>
    <strong>Official Java SDK for the WaveSpeedAI inference platform</strong>
  </p>

  <p>
    <a href="https://wavespeed.ai" target="_blank" rel="noopener noreferrer">🌐 Visit wavespeed.ai</a> •
    <a href="https://wavespeed.ai/docs">📖 Documentation</a> •
    <a href="https://github.com/WaveSpeedAI/wavespeed-java/issues">💬 Issues</a>
  </p>
</div>

---

## Introduction

**WaveSpeedAI** Java Client — Official Java SDK for **WaveSpeedAI** inference platform. This library provides a clean, unified, and high-performance API for your Java applications.

## Installation

Download `wavespeed-java-sdk-0.2.4.jar` from the
[v0.2.4 release](https://github.com/WaveSpeedAI/wavespeed-java/releases/tag/v0.2.4)
and add it to your application's classpath (coordinates: `ai.wavespeed:wavespeed-java-sdk:0.2.4`).

The SDK is not published to Maven Central (or any other registry) yet: the
release workflow currently only builds the JARs, so GitHub release assets are
the official distribution channel. The release also provides source and
Javadoc JARs.

## API Client

Run WaveSpeed AI models with a simple API:

```java
import ai.wavespeed.Wavespeed;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        Map<String, Object> output = Wavespeed.run(
            "wavespeed-ai/z-image/turbo",
            Map.of("prompt", "Cat")
        );

        System.out.println(output.get("outputs"));  // Output URL
    }
}
```

### Authentication

Set your API key via environment variable (You can get your API key from [https://wavespeed.ai/accesskey](https://wavespeed.ai/accesskey)):

```bash
export WAVESPEED_API_KEY="your-api-key"
```

Or pass it directly:

```java
import ai.wavespeed.api.Client;

Client client = new Client("your-api-key");
Map<String, Object> output = client.run(
    "wavespeed-ai/z-image/turbo",
    Map.of("prompt", "Cat")
);
```

### Options

```java
Map<String, Object> output = Wavespeed.run(
    "wavespeed-ai/z-image/turbo",
    Map.of("prompt", "Cat"),
    36000.0,  // timeout - Max wait time in seconds (default: 36000.0)
    1.0,      // pollInterval - Status check interval (default: 1.0)
    false,    // enableSyncMode - Best-effort sync result attempt (default: false)
    null      // maxRetries - Task-level retries (default: 0)
);
```

### Sync Mode

Use `enableSyncMode = true` to ask the API to wait for the result in the initial
request. If the server-side sync wait times out, the SDK throws an error with
the task ID/result URL; the task continues processing and can be queried later.

> **Note:** Not all models support sync mode. Check the model documentation for availability.

```java
Map<String, Object> output = Wavespeed.run(
    "wavespeed-ai/z-image/turbo",
    Map.of("prompt", "Cat"),
    true  // enableSyncMode
);
```

### Retry Configuration

Configure retries at the client level. Retries only apply to idempotent
result-query GET requests; the submission POST is sent exactly once and is
never retried, because a failed submission may still have created the task
on the server. When that happens the SDK throws
`ai.wavespeed.WavespeedSubmissionException`.

```java
import ai.wavespeed.api.Client;

// Simple retry configuration
Client client = new Client(
    "your-api-key",
    3,    // maxRetries - Task-level retries (default: 0)
    5,    // maxConnectionRetries - Result-query GET retries; the submission POST is never retried (default: 5)
    1.0   // retryInterval - Base delay between retries in seconds (default: 1.0)
);
```

### Upload Files

Upload images, videos, or audio files:

```java
import ai.wavespeed.Wavespeed;

String url = Wavespeed.upload("/path/to/image.png");
System.out.println(url);
```

### Getting Task ID and Debug Information

If you need access to the task ID for logging, tracking, or debugging, use `runNoThrow()` instead of `run()`. This method returns detailed information and does not throw exceptions:

```java
import ai.wavespeed.api.Client;

Client.RunNoThrowResult result = client.runNoThrow(model, input);

if (result.getOutputs() != null) {
    System.out.println("Success: " + result.getOutputs());
    System.out.println("Task ID: " + result.getDetail().getTaskId());  // For tracking/debugging
} else {
    System.out.println("Failed: " + result.getDetail().getError());
    System.out.println("Task ID: " + result.getDetail().getTaskId());  // Still available on failure
}
```

## Running Tests

```bash
# Run all tests
mvn test

# Run a single test file
mvn test -Dtest=ClientTest

# Run a specific test
mvn test -Dtest=ClientTest#testInitWithApiKey
```

## Environment Variables

### API Client

| Variable | Description |
|----------|-------------|
| `WAVESPEED_API_KEY` | WaveSpeed API key |
| `WAVESPEED_CLIENT_NAME` | Channel-attribution name sent as the `X-Client-Name` header. Takes precedence over `Client.setClientName()`; defaults to `wavespeed-java` |

## License

MIT
