package com.dss.test.ai.proxy.service;

import com.dss.test.ai.proxy.config.ProxyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API Key管理器
 * 实现API Key轮询调用,防止限流
 *
 * @author dongshoushan
 */
@Slf4j
@Component
public class ApiKeyManager {
    
    private final ProxyConfig proxyConfig;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    public ApiKeyManager(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
    
    /**
     * 获取下一个API Key(轮询方式)
     *
     * @return 下一个API Key
     * @author dongshoushan
     */
    public String getNextApiKey() {
        List<String> apiKeys = proxyConfig.getGeminiApiKeys();
        
        if (apiKeys == null || apiKeys.isEmpty()) {
            log.warn("未配置Gemini API Key列表,返回null");
            return null;
        }
        
        int index = currentIndex.getAndIncrement() % apiKeys.size();
        String apiKey = apiKeys.get(index);
        
        log.debug("使用API Key索引[{}]/共[{}]个", index, apiKeys.size());
        return apiKey;
    }
    
    /**
     * 替换Authorization头中的API Key
     *
     * @param originalAuth 原始Authorization头
     * @return 替换后的Authorization头
     * @author dongshoushan
     */
    public String replaceApiKey(String originalAuth) {
        if (originalAuth == null || originalAuth.trim().isEmpty()) {
            return originalAuth;
        }
        
        String newApiKey = getNextApiKey();
        if (newApiKey == null) {
            log.warn("无法获取新API Key,保留原始Authorization");
            return originalAuth;
        }
        
        // 替换Bearer token
        return "Bearer " + newApiKey;
    }
}
