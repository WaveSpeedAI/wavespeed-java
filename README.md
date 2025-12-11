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

Add this dependency to your project's `pom.xml`:

```xml
<dependency>
  <groupId>ai.wavespeed.maven</groupId>
  <artifactId>wavespeed</artifactId>
  <version>0.0.1</version>
</dependency>
```

### Gradle

Add this dependency to your project's `build.gradle`:

```groovy
dependencies {
  implementation "ai.wavespeed.maven:wavespeed:0.0.1"
}
```

## API Client

Run WaveSpeed AI models with a simple API:

```java
import ai.wavespeed.WaveSpeed;
import ai.wavespeed.openapi.client.model.Prediction;
import java.util.HashMap;
import java.util.Map;

WaveSpeed client = new WaveSpeed("your-api-key");
Map<String, Object> input = new HashMap<>();
input.put("prompt", "Cat");

try {
    Prediction result = client.run("wavespeed-ai/z-image/turbo", input);
    System.out.println(result.getOutputs().get(0)); // Output URL
} catch (Exception e) {
    e.printStackTrace();
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
// Custom poll interval and timeout (seconds)
WaveSpeed client = new WaveSpeed("your-api-key", 1.0, 36000.0);
```

### Upload Files

Upload images, videos, or audio files:

```java
WaveSpeed client = new WaveSpeed("your-api-key");

try {
    String url = client.upload("/path/to/image.png");
    System.out.println(url);
} catch (Exception e) {
    e.printStackTrace();
}
```

## Environment Variables

### API Client

| Variable | Description |
|----------|-------------|
| `WAVESPEED_API_KEY` | WaveSpeed API key |
| `WAVESPEED_BASE_URL` | API base URL without path (default: `https://api.wavespeed.ai`) |
| `WAVESPEED_POLL_INTERVAL` | Poll interval seconds for `run` (default: `1.0`) |
| `WAVESPEED_TIMEOUT` | Overall wait timeout seconds for `run` (default: `36000.0`) |

## Requirements

- Java 1.8+
- Maven 3.6+ or Gradle 7.2+

## License

Apache 2.0