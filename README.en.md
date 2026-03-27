# DSS AI Proxy

AI proxy service for forwarding Gemini API requests, supporting API Key rotation, model enforcement, auto-retry, thought_signature auto-injection, and more.

## Core Features

### 1. Model Enforcement
All model names in API requests are forcibly replaced with the model specified in the configuration file, ensuring optimal model usage.

**Configuration Example:**
```yaml
ai:
  proxy:
    models:
      - gemini-3.1-flash-lite-preview  # 1st priority
      - gemini-3-flash-preview         # 2nd priority
      - gemini-2.5-flash               # 3rd priority
      - gemini-2.5-flash-lite          # 4th priority
```

### 2. Intelligent Retry Mechanism
When a model call fails, the system automatically switches to the next model and retries immediately until success or all models fail.

**Retry Flow:**
```
Model A fails → Immediately switch to Model B and retry → Fails → Switch to Model C and retry → Success
```

### 3. Scheduled Model Reset
Automatically resets to the first model every 4 hours to ensure optimal performance.

**Reset Schedule:** 0:00, 4:00, 8:00, 12:00, 16:00, 20:00

### 4. API Key Rotation
Supports multiple API Keys rotation to avoid single key rate limiting and improve overall quota.

## Requirements

| Component | Version |
|-----------|---------|
| JDK | 21 |
| Maven | 3.6+ |
| Docker | Any |

**Important**: The deployment environment must be able to access Google websites (Google/Gemini API). If direct access is not available, proxy configuration is required.

## Quick Start

### 1. Build

```bash
mvn clean package -DskipTests
```

### 2. Build Docker Image

```bash
docker build -t dss-ai-proxy:latest .
```

### 3. Run Container

```bash
docker run -d \
  --name dss-ai-proxy \
  --restart=always \
  -p 30089:30089 \
  -v /home/dss-ai-proxy/config:/app/config \
  dss-ai-proxy:latest
```

## Configuration

Create `application.yml` in the mounted directory:

```yaml
server:
  port: 30089

logging:
  level:
    com.dss.test.ai.proxy: INFO

ai:
  proxy:
    target-base-url: https://generativelanguage.googleapis.com/v1beta/openai
    # Proxy configuration (optional, depends on network environment)
    # If the deployment environment can directly access Google, no proxy is needed
    proxy-host: proxy.example.com
    proxy-port: 8080
    proxy-username: your-username
    proxy-password: your-password
    connect-timeout: 30000
    read-timeout: 120000
    gemini-api-keys:
      - your-api-key-1
      - your-api-key-2
    # Model list (sorted by priority, supports auto-switching and retry)
    models:
      - gemini-3.1-flash-lite-preview
      - gemini-3-flash-preview
      - gemini-2.5-flash
      - gemini-2.5-flash-lite
```

### Proxy Configuration

| Property | Required | Description |
|----------|----------|-------------|
| proxy-host | No | Proxy server address, direct connection if not configured |
| proxy-port | No | Proxy server port |
| proxy-username | No | Proxy authentication username |
| proxy-password | No | Proxy authentication password |

### Model Configuration

| Property | Required | Description |
|----------|----------|-------------|
| models | Yes | Model list, sorted by priority, supports auto-switching and retry |

**Model Switching Strategy:**
- 1st model fails → Auto switch to 2nd model and retry
- 2nd model fails → Auto switch to 3rd model and retry
- 3rd model fails → Auto switch to 4th model and retry
- 4th model fails → Return error (all models failed)

**Scheduled Reset:** Automatically resets to the 1st model every 4 hours

## Model Quota Information

### Configured Model List

The system is configured with the following 4 models, sorted by priority:

| Model | Category | RPM | TPM | RPD |
|-------|----------|-----|-----|-----|
| Gemini 3.1 Flash Lite | Text Output Model | 15 | 250K | 500 |
| Gemini 3 Flash | Text Output Model | 5 | 250K | 20 |
| Gemini 2.5 Flash | Text Output Model | 5 | 250K | 20 |
| Gemini 2.5 Flash Lite | Text Output Model | 10 | 250K | 20 |

**Quota Explanation**:
- **RPM** (Requests Per Minute): Maximum requests per minute
- **TPM** (Tokens Per Minute): Maximum tokens per minute
- **RPD** (Requests Per Day): Maximum requests per day

### Overall Available Quota

Since the system supports automatic model switching and retry, the overall available quota is the sum of all model quotas:

| Metric | Single Model Max | Overall Available | Description |
|--------|------------------|-------------------|-------------|
| RPM | 15 | **35** | 15 + 5 + 5 + 10 |
| TPM | 250K | **1M** | 250K × 4 |
| RPD | 500 | **560** | 500 + 20 + 20 + 20 |

**Advantages**:
- Quota stacking achieved through model switching mechanism
- Automatic switch to next model when rate limit is reached
- Overall available quota far exceeds single model, providing more stable service

### API Key Rotation Strategy

To avoid API rate limiting:

1. Create **10 projects** in Google Cloud
2. Request **1 API Key** per project
3. Configure 10 API Keys for rotation, actual quota will be **x10**:
   - RPM: 150
   - TPM: 2.5M
   - RPD: 5000

### Related Links

- Project Management: https://aistudio.google.com/projects
- Rate Limit Monitoring: https://aistudio.google.com/rate-limit?timeRange=last-28-days

## Common Commands

```bash
# View logs
docker logs -f dss-ai-proxy

# Restart service
docker restart dss-ai-proxy

# Enter container
docker exec -it dss-ai-proxy /bin/bash
```

## Notes

1. Ensure the deployment environment can access Google (directly or via proxy)
2. Ensure port 30089 is not occupied
3. Configuration file contains sensitive information, keep it secure
4. Rotate API Keys regularly to avoid rate limiting

## Author

dongshoushan
