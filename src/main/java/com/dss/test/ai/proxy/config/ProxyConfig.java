package com.dss.test.ai.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI代理配置属性类
 * 从application.yml读取配置
 * 
 * @author dongshoushan
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.proxy")
public class ProxyConfig {
    
    /**
     * 目标AI服务基础URL
     */
    private String targetBaseUrl;
    
    /**
     * 华为代理主机（非必填）
     * 如果部署环境可以直连谷歌，则不需要配置代理
     */
    private String proxyHost;
    
    /**
     * 华为代理端口（非必填）
     */
    private Integer proxyPort;
    
    /**
     * 华为代理用户名（非必填）
     */
    private String proxyUsername;
    
    /**
     * 华为代理密码（非必填）
     */
    private String proxyPassword;
    
    /**
     * 连接超时时间(毫秒)
     */
    private int connectTimeout = 30000;
    
    /**
     * 读取超时时间(毫秒)
     * GLM响应可能需要5-10秒,设置为120秒
     */
    private int readTimeout = 120000;
    
    /**
     * Gemini API Key列表
     * 用于轮询调用,防止限流
     */
    private List<String> geminiApiKeys;
    
    /**
     * 判断是否启用代理
     * 当proxyHost和proxyPort都配置时才启用代理
     *
     * @return true-启用代理, false-不启用代理
     * @author dongshoushan
     */
    public boolean isProxyEnabled() {
        return proxyHost != null && !proxyHost.isBlank() 
               && proxyPort != null && proxyPort > 0;
    }
    
    /**
     * 判断是否配置了代理认证
     * 当proxyUsername和proxyPassword都配置时才启用认证
     *
     * @return true-启用认证, false-不启用认证
     * @author dongshoushan
     */
    public boolean isProxyAuthEnabled() {
        return isProxyEnabled() 
               && proxyUsername != null && !proxyUsername.isBlank()
               && proxyPassword != null && !proxyPassword.isBlank();
    }
}
