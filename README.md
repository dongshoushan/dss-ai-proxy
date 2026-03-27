# DSS AI Proxy

AI代理服务，用于转发Gemini API请求，支持API Key轮询、模型强制替换、自动重试、thought_signature自动注入等功能。

## 核心功能

### 1. 模型强制替换
所有API请求中的模型名称都会被强制替换为配置文件中指定的模型，确保使用最优模型。

**配置示例：**
```yaml
ai:
  proxy:
    models:
      - gemini-3.1-flash-lite-preview  # 第1优先级
      - gemini-3-flash-preview         # 第2优先级
      - gemini-2.5-flash               # 第3优先级
      - gemini-2.5-flash-lite          # 第4优先级
```

### 2. 智能重试机制
当某个模型调用失败时，系统会自动切换到下一个模型并立即重试，直到成功或所有模型都失败。

**重试流程：**
```
模型A调用失败 → 立即切换到模型B重试 → 失败 → 切换到模型C重试 → 成功返回
```

### 3. 定时模型重置
每4小时自动重置为第一个模型，确保使用最优性能模型。

**重置时间点：** 0:00, 4:00, 8:00, 12:00, 16:00, 20:00

### 4. API Key轮询
支持配置多个API Key轮询使用，避免单Key限流，提升整体配额。

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
    # 模型列表（按优先级排序，支持自动切换和重试）
    models:
      - gemini-3.1-flash-lite-preview
      - gemini-3-flash-preview
      - gemini-2.5-flash
      - gemini-2.5-flash-lite
```

### 代理配置说明

| 配置项 | 必填 | 说明 |
|--------|------|------|
| proxy-host | 否 | 代理服务器地址，不配置则直连 |
| proxy-port | 否 | 代理服务器端口 |
| proxy-username | 否 | 代理认证用户名 |
| proxy-password | 否 | 代理认证密码 |

### 模型配置说明

| 配置项 | 必填 | 说明 |
|--------|------|------|
| models | 是 | 模型列表，按优先级排序，支持自动切换和重试 |

**模型切换策略：**
- 第1个模型异常 → 自动切换到第2个模型重试
- 第2个模型异常 → 自动切换到第3个模型重试
- 第3个模型异常 → 自动切换到第4个模型重试
- 第4个模型异常 → 返回错误（所有模型都失败）

**定时重置：** 每4小时自动重置为第1个模型

## 模型配额说明

### 配置的模型列表

系统配置了以下4个模型，按优先级排序：

| 模型 | 类别 | RPM | TPM | RPD |
|------|------|-----|-----|-----|
| Gemini 3.1 Flash Lite | 文本输出模型 | 15 | 250K | 500 |
| Gemini 3 Flash | 文本输出模型 | 5 | 250K | 20 |
| Gemini 2.5 Flash | 文本输出模型 | 5 | 250K | 20 |
| Gemini 2.5 Flash Lite | 文本输出模型 | 10 | 250K | 20 |

**配额说明**：
- **RPM** (Requests Per Minute)：每分钟最大请求数
- **TPM** (Tokens Per Minute)：每分钟最大Token数
- **RPD** (Requests Per Day)：每天最大请求数

### 整体可用额度

由于系统支持模型自动切换和重试，整体可用额度为所有模型配额的总和：

| 指标 | 单模型最大值 | 整体可用额度 | 说明 |
|------|-------------|-------------|------|
| RPM | 15 | **35** | 15 + 5 + 5 + 10 |
| TPM | 250K | **1M** | 250K × 4 |
| RPD | 500 | **560** | 500 + 20 + 20 + 20 |

**优势**：
- 通过模型切换机制，实现了配额的叠加
- 当某个模型达到限流时，自动切换到下一个模型
- 整体可用额度远超单个模型，提供更稳定的服务

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
