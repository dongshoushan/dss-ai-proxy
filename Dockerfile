# JRE 21镜像
FROM eclipse-temurin:21-jre
# 切换到root用户
USER root

# 设置工作目录
WORKDIR /app

# 设置时区环境变量
ENV TZ=Asia/Shanghai

# 创建配置文件目录
RUN mkdir -p /app/config

# 复制jar包
COPY dss-ai-proxy-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 30089

# 启动应用,使用外部配置文件
ENTRYPOINT ["java", "-jar", "-Dspring.config.location=/app/config/application.yml", "app.jar"]
