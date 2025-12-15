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
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## API Client

Run WaveSpeed AI models with a simple API:

```java
import ai.wavespeed.WaveSpeed;
import ai.wavespeed.openapi.client.model.Prediction;
import java.util.HashMap;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        WaveSpeed client = new WaveSpeed("your-api-key");

        try {
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", "Cat");

            Prediction result = client.run("wavespeed-ai/z-image/turbo", input);

            System.out.println(result.getOutputs().get(0));  // Output URL

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
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
WaveSpeed client = new WaveSpeed("your-api-key");
```

### Options

```java
Prediction result = client.run(
    "wavespeed-ai/z-image/turbo",
    input,
    300.0,     // Max wait time in seconds (default: 36000.0)
    2.0,       // Status check interval (default: 1.0)
    false      // Single request mode, no polling (default: false)
);
```

### Sync Mode

Use `enableSyncMode: true` for a single request that waits for the result (no polling).

> **Note:** Not all models support sync mode. Check the model documentation for availability.

```java
Prediction result = client.run(
    "wavespeed-ai/z-image/turbo",
    input,
    null,      // use default timeout
    null,      // use default poll interval
    true       // enable sync mode
);
```

### Retry Configuration

Configure retries when creating the client:

```java
WaveSpeed client = new WaveSpeed(
    "your-api-key",
    null,      // use default poll interval
    null,      // use default timeout
    0,         // Task-level retries (default: 0)
    5,         // HTTP connection retries (default: 5)
    1.0        // Base delay between retries in seconds (default: 1.0)
);
```

### Upload Files

Upload images, videos, or audio files:

```java
import ai.wavespeed.WaveSpeed;

WaveSpeed client = new WaveSpeed("your-api-key");

try {
    String downloadUrl = client.upload("/path/to/image.png");
    System.out.println(downloadUrl);
} catch (Exception e) {
    System.err.println("Upload failed: " + e.getMessage());
}
```

## Environment Variables

### API Client

| Variable | Description |
|----------|-------------|
| `WAVESPEED_API_KEY` | WaveSpeed API key |

## License

MIT
