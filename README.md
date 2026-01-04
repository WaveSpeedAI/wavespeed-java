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

## Installation

### Maven

```xml
<dependency>
  <groupId>ai.wavespeed</groupId>
  <artifactId>wavespeed-java-sdk</artifactId>
  <version>0.2.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'ai.wavespeed:wavespeed-java-sdk:0.2.0'
```

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
    false,    // enableSyncMode - Single request mode, no polling (default: false)
    null      // maxRetries - Task-level retries (default: 0)
);
```

### Sync Mode

Use `enableSyncMode = true` for a single request that waits for the result (no polling).

> **Note:** Not all models support sync mode. Check the model documentation for availability.

```java
Map<String, Object> output = Wavespeed.run(
    "wavespeed-ai/z-image/turbo",
    Map.of("prompt", "Cat"),
    true  // enableSyncMode
);
```

### Retry Configuration

Configure retries at the client level:

```java
import ai.wavespeed.api.Client;

// Simple retry configuration
Client client = new Client(
    "your-api-key",
    3,    // maxRetries - Task-level retries (default: 0)
    5,    // maxConnectionRetries - HTTP connection retries (default: 5)
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

## Environment Variables

### API Client

| Variable | Description |
|----------|-------------|
| `WAVESPEED_API_KEY` | WaveSpeed API key |

## License

MIT
