package com.dss.test.ai.proxy.controller;

import com.dss.test.ai.proxy.service.AiProxyService;
import com.dss.test.ai.proxy.service.ModelSwitchManager;
import com.dss.test.ai.proxy.service.ThoughtSignatureManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI代理控制器
 * 处理OpenAI兼容的API请求并转发到目标AI服务
 *
 * @author dss
 */
@RestController
@RequestMapping("/v1")
public class AiProxyController {

    private static final Logger log = LoggerFactory.getLogger(AiProxyController.class);

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String STORE_FIELD = "store";
    private static final int MAX_VALUE_PREVIEW_LENGTH = 50;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiProxyService aiProxyService;
    private final ThoughtSignatureManager signatureManager;
    private final ModelSwitchManager modelSwitchManager;

    public AiProxyController(AiProxyService aiProxyService, 
                           ThoughtSignatureManager signatureManager,
                           ModelSwitchManager modelSwitchManager) {
        this.aiProxyService = aiProxyService;
        this.signatureManager = signatureManager;
        this.modelSwitchManager = modelSwitchManager;
    }    
    /**
     * 代理chat completions接口
     *
     * @param headers 请求头
     * @param body 请求体
     * @return 目标服务的响应
     * @author dongshoushan
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<String> chatCompletions(
            @RequestHeader Map<String, String> headers,
            @RequestBody String body) {

        log.info("收到chat completions请求");
        log.debug("请求头: {}", headers);
        log.debug("请求体摘要: {}", formatRequestBodySummary(body));

        // 使用ThoughtSignatureManager处理请求体
        String processedBody = signatureManager.processRequestBody(body);
        
        // 移除store字段
        processedBody = removeStoreField(processedBody);
        
        // 强制替换模型名称为当前模型
        String currentModel = modelSwitchManager.getCurrentModel();
        processedBody = modelSwitchManager.replaceModelInRequest(processedBody, currentModel);
        log.info("已强制替换模型为: {}", currentModel);

        HttpHeaders httpHeaders = convertHeaders(headers);

        ResponseEntity<String> response = aiProxyService.proxyRequest(
            CHAT_COMPLETIONS_PATH, httpHeaders, processedBody, currentModel);

        // 从响应中提取thought_signature
        if (response.getBody() != null) {
            String signature = signatureManager.extractSignature(response.getBody());
            if (signature != null) {
                log.info("已提取thought_signature用于后续请求");
            }
        }

        return response;
    }    
    /**
     * 转换请求头
     *
     * @param headers 原始请求头
     * @return 转换后的HttpHeaders
     * @author dongshoushan
     */
    private HttpHeaders convertHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach((key, value) -> httpHeaders.put(key, List.of(value)));
        return httpHeaders;
    }

    /**
     * 移除请求体中的store字段
     * Gemini API不支持store字段,需要移除
     *
     * @param body 原始请求体JSON字符串
     * @return 移除store字段后的请求体JSON字符串
     * @author dongshoushan
     */
    private String removeStoreField(String body) {
        try {
            JsonNode json = OBJECT_MAPPER.readTree(body);
            if (json instanceof ObjectNode objectNode && json.has(STORE_FIELD)) {
                objectNode.remove(STORE_FIELD);
                log.info("已移除不支持的store字段");
                return OBJECT_MAPPER.writeValueAsString(objectNode);
            }
            return body;
        } catch (Exception e) {
            log.error("处理请求体失败,将使用原始请求体: {}", e.getMessage());
            return body;
        }
    }
    
    /**
     * 格式化请求体摘要信息
     * 打印所有key,value只显示前50个字符,保持JSON格式完整性
     *
     * @param body 请求体JSON字符串
     * @return 格式化后的摘要信息
     * @author dongshoushan
     */
    private String formatRequestBodySummary(String body) {
        try {
            JsonNode json = OBJECT_MAPPER.readTree(body);
            ObjectNode summaryNode = OBJECT_MAPPER.createObjectNode();

            json.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                String valueStr = value.toString();

                if (valueStr.length() > MAX_VALUE_PREVIEW_LENGTH) {
                    String truncated = valueStr.substring(0, MAX_VALUE_PREVIEW_LENGTH) + "...";
                    summaryNode.put(key, truncated);
                } else {
                    summaryNode.set(key, value);
                }
            });

            return OBJECT_MAPPER.writeValueAsString(summaryNode);
        } catch (Exception e) {
            log.error("解析请求体失败: {}", e.getMessage());
            return body.length() > MAX_VALUE_PREVIEW_LENGTH
                    ? body.substring(0, MAX_VALUE_PREVIEW_LENGTH) + "..."
                    : body;
        }
    }
}
