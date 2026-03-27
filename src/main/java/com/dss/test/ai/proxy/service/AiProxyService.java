package com.dss.test.ai.proxy.service;

import com.dss.test.ai.proxy.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * AI代理服务
 * 负责将请求转发到目标AI服务
 *
 * @author dss
 */
@Service
public class AiProxyService {
    
    private static final Logger log = LoggerFactory.getLogger(AiProxyService.class);
    
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    private final RestTemplate restTemplate;
    private final ProxyConfig proxyConfig;
    private final ApiKeyManager apiKeyManager;
    private final ModelSwitchManager modelSwitchManager;
    
    public AiProxyService(RestTemplate restTemplate, ProxyConfig proxyConfig, 
                         ApiKeyManager apiKeyManager, ModelSwitchManager modelSwitchManager) {
        this.restTemplate = restTemplate;
        this.proxyConfig = proxyConfig;
        this.apiKeyManager = apiKeyManager;
        this.modelSwitchManager = modelSwitchManager;
    }
    
    /**
     * 代理转发请求到目标AI服务
     * 当请求失败时会自动切换到下一个模型并重试
     * 会尝试所有模型，直到成功或所有模型都失败
     *
     * @param path 请求路径
     * @param headers 请求头
     * @param body 请求体(JSON字符串)
     * @param currentModel 当前使用的模型名称
     * @return 目标服务的响应
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public ResponseEntity<String> proxyRequest(String path, HttpHeaders headers, 
                                              String body, String currentModel) {
        String targetUrl = buildTargetUrl(path);
        log.info("代理请求到目标URL: {}, 使用模型: {}", targetUrl, currentModel);
        
        HttpHeaders proxyHeaders = buildProxyHeaders(headers);
        HttpEntity<String> entity = new HttpEntity<>(body, proxyHeaders);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            log.info("目标服务响应状态: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("代理请求失败, URL: {}, 模型: {}, 错误: {}", 
                     targetUrl, currentModel, e.getMessage(), e);
            
            // 切换到下一个模型并重试
            return retryWithNextModel(path, headers, body, currentModel, e);
        }
    }
    
    /**
     * 使用下一个模型重试请求
     * 会尝试所有模型，直到成功或所有模型都失败
     *
     * @param path 请求路径
     * @param headers 请求头
     * @param body 请求体(JSON字符串)
     * @param failedModel 失败的模型名称
     * @param originalException 原始异常
     * @return 目标服务的响应
     * @author dongshoushan
     * @工号 dwx1402878
     */
    private ResponseEntity<String> retryWithNextModel(String path, HttpHeaders headers, 
                                                      String body, String failedModel, 
                                                      Exception originalException) {
        int modelCount = modelSwitchManager.getModelCount();
        int retryCount = 0;
        
        while (retryCount < modelCount - 1) {
            // 切换到下一个模型
            String nextModel = modelSwitchManager.switchToNextModel();
            log.info("模型 {} 异常，切换到下一个模型: {} 进行重试", failedModel, nextModel);
            
            // 替换请求体中的模型名称
            String newBody = modelSwitchManager.replaceModelInRequest(body, nextModel);
            
            String targetUrl = buildTargetUrl(path);
            HttpHeaders proxyHeaders = buildProxyHeaders(headers);
            HttpEntity<String> entity = new HttpEntity<>(newBody, proxyHeaders);
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
                );
                
                log.info("使用模型 {} 重试成功, 响应状态: {}", nextModel, response.getStatusCode());
                return response;
            } catch (Exception e) {
                log.error("使用模型 {} 重试失败: {}", nextModel, e.getMessage());
                failedModel = nextModel;
                retryCount++;
            }
        }
        
        // 所有模型都失败，抛出原始异常
        log.error("所有模型都已尝试失败，共尝试 {} 个模型", modelCount);
        throw new RuntimeException("所有模型尝试失败", originalException);
    }
    
    /**
     * 构建目标URL
     *
     * @param path 请求路径
     * @return 完整的目标URL
     * @author dongshoushan
     */
    private String buildTargetUrl(String path) {
        return proxyConfig.getTargetBaseUrl() + path;
    }
    
    /**
     * 构建代理请求头
     * 保留原始Authorization和Content-Type
     * 强制替换Authorization中的API Key为配置文件中的轮询Key
     *
     * @param originalHeaders 原始请求头
     * @return 代理请求头
     * @author dongshoushan
     */
    private HttpHeaders buildProxyHeaders(HttpHeaders originalHeaders) {
        HttpHeaders proxyHeaders = new HttpHeaders();
        
        // 复制Content-Type
        if (originalHeaders.containsKey(CONTENT_TYPE_HEADER)) {
            proxyHeaders.put(CONTENT_TYPE_HEADER, originalHeaders.get(CONTENT_TYPE_HEADER));
        }
        
        // 强制替换Authorization中的API Key
        if (originalHeaders.containsKey(AUTHORIZATION_HEADER)) {
            List<String> authValues = originalHeaders.get(AUTHORIZATION_HEADER);
            if (authValues != null && !authValues.isEmpty()) {
                String originalAuth = authValues.get(0);
                String newAuth = apiKeyManager.replaceApiKey(originalAuth);
                proxyHeaders.put(AUTHORIZATION_HEADER, List.of(newAuth));
                log.info("已替换API Key为配置文件中的轮询Key");
            }
        }
        
        return proxyHeaders;
    }
}
