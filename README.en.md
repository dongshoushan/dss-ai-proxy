# DSS AI Proxy

AI proxy service for forwarding Gemini API requests, supporting API Key rotation, thought_signature auto-injection, and more.

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
    # 模型列表(按顺序切换，第一个异常则使用第二个，以此类推)
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

## API Key Configuration

### Model

model: **gemini-3.1-flash-lite-preview**

| Model | Category | RPM | TPM | RPD |
|-------|----------|-----|-----|-----|
| Gemini 3.1 Flash Lite | Text Output Model | 15 | 250K | 500 |

**Quota Explanation**:
- **RPM** (Requests Per Minute): Maximum requests per minute
- **TPM** (Tokens Per Minute): Maximum tokens per minute
- **RPD** (Requests Per Day): Maximum requests per day

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
