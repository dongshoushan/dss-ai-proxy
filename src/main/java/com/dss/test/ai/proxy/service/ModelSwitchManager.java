package com.dss.test.ai.proxy.service;

import com.dss.test.ai.proxy.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模型切换管理器
 * 负责管理多个模型的切换策略，当某个模型返回异常时自动切换到下一个模型
 * 每天早上6点会强制重置为第一个模型
 *
 * @author dongshoushan
 * @工号 dwx1402878
 */
@Component
public class ModelSwitchManager {
    
    private static final Logger log = LoggerFactory.getLogger(ModelSwitchManager.class);
    
    private final ProxyConfig proxyConfig;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    /**
     * 构造函数
     *
     * @param proxyConfig 代理配置
     */
    public ModelSwitchManager(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
    
    /**
     * 获取当前使用的模型名称
     *
     * @return 当前模型名称
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public String getCurrentModel() {
        List<String> models = proxyConfig.getModels();
        if (models == null || models.isEmpty()) {
            log.warn("模型列表为空，返回默认模型");
            return "gemini-3.1-flash-lite-preview";
        }
        int index = currentIndex.get() % models.size();
        return models.get(index);
    }
    
    /**
     * 切换到下一个模型
     * 当当前模型返回异常时调用此方法
     *
     * @return 切换后的模型名称
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public String switchToNextModel() {
        List<String> models = proxyConfig.getModels();
        if (models == null || models.isEmpty()) {
            log.warn("模型列表为空，无法切换模型");
            return "gemini-3.1-flash-lite-preview";
        }
        
        int oldIndex = currentIndex.get();
        int newIndex = (oldIndex + 1) % models.size();
        currentIndex.set(newIndex);
        
        String oldModel = models.get(oldIndex % models.size());
        String newModel = models.get(newIndex);
        
        log.info("模型切换: {} -> {}", oldModel, newModel);
        return newModel;
    }
    
    /**
     * 重置为第一个模型
     * 每天早上6点定时任务调用此方法
     *
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public void resetToFirstModel() {
        List<String> models = proxyConfig.getModels();
        if (models == null || models.isEmpty()) {
            log.warn("模型列表为空，无法重置模型");
            return;
        }
        
        int oldIndex = currentIndex.get();
        currentIndex.set(0);
        
        if (oldIndex != 0) {
            String oldModel = models.get(oldIndex % models.size());
            String newModel = models.get(0);
            log.info("定时重置模型: {} -> {}", oldModel, newModel);
        } else {
            log.info("当前已是第一个模型，无需重置");
        }
    }
    
    /**
     * 替换请求体中的模型名称
     * 将请求体中的model字段强制替换为当前模型
     *
     * @param requestBody 原始请求体JSON字符串
     * @param currentModel 当前模型名称
     * @return 替换后的请求体JSON字符串
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public String replaceModelInRequest(String requestBody, String currentModel) {
        if (requestBody == null || requestBody.isBlank()) {
            return requestBody;
        }
        
        try {
            // 使用正则表达式替换model字段
            // 匹配 "model": "任意内容" 并替换为当前模型
            String pattern = "\"model\"\\s*:\\s*\"[^\"]*\"";
            String replacement = "\"model\": \"" + currentModel + "\"";
            return requestBody.replaceAll(pattern, replacement);
        } catch (Exception e) {
            log.error("替换模型名称失败: {}", e.getMessage(), e);
            return requestBody;
        }
    }
    
    /**
     * 获取模型列表大小
     *
     * @return 模型列表大小
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public int getModelCount() {
        List<String> models = proxyConfig.getModels();
        return models != null ? models.size() : 0;
    }
    
    /**
     * 获取当前模型索引
     *
     * @return 当前模型索引
     * @author dongshoushan
     * @工号 dwx1402878
     */
    public int getCurrentIndex() {
        return currentIndex.get();
    }
}
