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
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-java-client</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.openapitools:openapi-java-client:0.1.0-SNAPSHOT'
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
            // Prepare input
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", "A beautiful sunset over mountains");

            // Run model and wait for completion
            Prediction result = client.run("wavespeed-ai/z-image/turbo", input);

            System.out.println("Output: " + result.getOutputs());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### Authentication

Set your API key via environment variable:

```bash
export WAVESPEED_API_KEY="your-api-key"
```

Or pass it directly:

```java
WaveSpeed client = new WaveSpeed("your-api-key");
```

You can get your API key from [https://wavespeed.ai/accesskey](https://wavespeed.ai/accesskey).

### Options

```java
Prediction result = client.run(
    "wavespeed-ai/z-image/turbo",
    input,
    300.0,     // timeout in seconds (default: 36000.0)
    2.0        // poll interval in seconds (default: 1.0)
);
```

## Upload Files

Upload images, videos, or audio files:

```java
import ai.wavespeed.WaveSpeed;

WaveSpeed client = new WaveSpeed("your-api-key");

try {
    String downloadUrl = client.upload("/path/to/image.png");
    System.out.println("Upload URL: " + downloadUrl);
} catch (Exception e) {
    System.err.println("Upload failed: " + e.getMessage());
}
```

## Building & Testing

### Run Tests
```bash
mvn test
```

### Build JAR
```bash
mvn clean package
```

### Install Locally
```bash
mvn clean install
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `WAVESPEED_API_KEY` | WaveSpeed API key |
| `WAVESPEED_BASE_URL` | API base URL (optional) |

## License

MIT