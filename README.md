# DSS AI Proxy

AI代理服务，用于转发Gemini API请求，支持API Key轮询、thought_signature自动注入等功能。

## 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 21 |
| Maven | 3.6+ |
| Docker | 任意版本 |

**重要**：部署环境需要能够访问谷歌网站（Google/Gemini API）。如果网络环境无法直连，需配置代理。

## 快速开始

### 1. 编译打包

```bash
mvn clean package -DskipTests
```

### 2. 构建Docker镜像

```bash
docker build -t dss-ai-proxy:latest .
```

### 3. 启动容器

```bash
docker run -d \
  --name dss-ai-proxy \
  --restart=always \
  -p 30089:30089 \
  -v /home/dss-ai-proxy/config:/app/config \
  dss-ai-proxy:latest
```

## 配置说明

首次部署需在挂载目录创建 `application.yml` 配置文件：

```yaml
server:
  port: 30089

logging:
  level:
    com.dss.test.ai.proxy: INFO

ai:
  proxy:
    target-base-url: https://generativelanguage.googleapis.com/v1beta/openai
    # 代理配置（非必填，根据网络环境决定）
    # 如果部署环境可以直连谷歌，则不需要配置代理
    proxy-host: proxyuk.huawei.com
    proxy-port: 8080
    proxy-username: your-username
    proxy-password: your-password
    connect-timeout: 30000
    read-timeout: 120000
    gemini-api-keys:
      - your-api-key-1
      - your-api-key-2
```

### 代理配置说明

| 配置项 | 必填 | 说明 |
|--------|------|------|
| proxy-host | 否 | 代理服务器地址，不配置则直连 |
| proxy-port | 否 | 代理服务器端口 |
| proxy-username | 否 | 代理认证用户名 |
| proxy-password | 否 | 代理认证密码 |

## API Key 配置建议

### 推荐模型

推荐使用 **gemini-3.1-flash-lite-preview** 模型，该模型配额如下：

| 模型 | 类别 | RPM | TPM | RPD |
|------|------|-----|-----|-----|
| Gemini 3.1 Flash Lite | 文本输出模型 | 15 | 250K | 500 |

**配额说明**：
- **RPM** (Requests Per Minute)：每分钟最大请求数
- **TPM** (Tokens Per Minute)：每分钟最大Token数
- **RPD** (Requests Per Day)：每天最大请求数

### API Key 轮询策略

为避免API限流，建议：

1. 在谷歌云创建 **10个项目**
2. 每个项目申请 **1个API Key**
3. 配置10个API Key轮询使用，实际配额将 **x10**：
   - RPM: 150
   - TPM: 2.5M
   - RPD: 5000

### 相关链接

- 项目管理：https://aistudio.google.com/projects
- 限流监控：https://aistudio.google.com/rate-limit?timeRange=last-28-days

## 常用命令

```bash
# 查看日志
docker logs -f dss-ai-proxy

# 重启服务
docker restart dss-ai-proxy

# 进入容器
docker exec -it dss-ai-proxy /bin/bash
```

## 注意事项

1. 确保部署环境能访问谷歌网站（直连或通过代理）
2. 确保30089端口未被占用
3. 配置文件包含敏感信息，请妥善保管
4. 定期轮换API Key，避免限流

## 作者

dongshoushan
