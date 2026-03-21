package com.dss.test.ai.proxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thought Signature管理器
 * 管理Gemini工具调用所需的thought_signature
 *
 * @author dongshoushan
 */
@Component
public class ThoughtSignatureManager {

    private static final Logger log = LoggerFactory.getLogger(ThoughtSignatureManager.class);

    private static final String THOUGHT_SIGNATURE = "thought_signature";
    private static final String SKIP_VALIDATOR = "skip_thought_signature_validator";
    private static final String CHOICES = "choices";
    private static final String MESSAGE = "message";
    private static final String TOOL_CALLS = "tool_calls";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 会话签名存储
     * Key: 会话标识
     * Value: thought_signature值
     */
    private final Map<String, String> sessionSignatures = new ConcurrentHashMap<>();

    /**
     * 处理请求体,为tool_calls添加thought_signature
     *
     * @param body 原始请求体
     * @return 处理后的请求体
     * @author dongshoushan
     */
    public String processRequestBody(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (!(json instanceof ObjectNode objectNode)) {
                return body;
            }

            boolean modified = false;
            JsonNode messages = json.get("messages");
            if (messages != null && messages.isArray()) {
                int msgIndex = 0;
                for (JsonNode message : messages) {
                    if (message instanceof ObjectNode msgNode) {
                        modified |= processToolCalls(msgNode, msgIndex);
                    }
                    msgIndex++;
                }
            }

            if (modified) {
                String result = objectMapper.writeValueAsString(objectNode);
                log.info("请求体处理完成");
                return result;
            }
            return body;
        } catch (Exception e) {
            log.error("处理请求体失败: {}", e.getMessage());
            return body;
        }
    }

    /**
     * 处理消息中的tool_calls
     * 根据Google Gemini API文档,thought_signature应放在extra_content.google.thought_signature中
     *
     * @param messageNode 消息节点
     * @param msgIndex 消息索引
     * @return 是否修改
     * @author dongshoushan
     */
    private boolean processToolCalls(ObjectNode messageNode, int msgIndex) {
        JsonNode toolCalls = messageNode.get(TOOL_CALLS);
        if (toolCalls == null || !toolCalls.isArray()) {
            return false;
        }

        boolean modified = false;
        int toolIndex = 0;
        for (JsonNode toolCall : toolCalls) {
            if (toolCall instanceof ObjectNode toolCallNode) {
                // 检查是否已有extra_content节点
                JsonNode extraContent = toolCallNode.get("extra_content");
                ObjectNode extraContentNode;
                
                if (extraContent == null || !(extraContent instanceof ObjectNode)) {
                    // 创建extra_content节点
                    extraContentNode = objectMapper.createObjectNode();
                    toolCallNode.set("extra_content", extraContentNode);
                } else {
                    extraContentNode = (ObjectNode) extraContent;
                }
                
                // 检查是否已有google节点
                JsonNode google = extraContentNode.get("google");
                ObjectNode googleNode;
                
                if (google == null || !(google instanceof ObjectNode)) {
                    // 创建google节点
                    googleNode = objectMapper.createObjectNode();
                    extraContentNode.set("google", googleNode);
                } else {
                    googleNode = (ObjectNode) google;
                }
                
                // 在extra_content.google中添加thought_signature
                if (!googleNode.has(THOUGHT_SIGNATURE)) {
                    googleNode.put(THOUGHT_SIGNATURE, SKIP_VALIDATOR);
                    modified = true;
                }
            }
            toolIndex++;
        }
        return modified;
    }

    /**
     * 从响应中提取thought_signature
     * 从extra_content.google.thought_signature中提取
     *
     * @param responseBody 响应体
     * @return 提取的签名(如果有)
     * @author dongshoushan
     */
    public String extractSignature(String responseBody) {
        // 空值检查
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }

        // 跳过非JSON响应(如SSE流式数据)
        String trimmedBody = responseBody.trim();
        if (!trimmedBody.startsWith("{") && !trimmedBody.startsWith("[")) {
            log.debug("响应体不是JSON格式,跳过thought_signature提取");
            return null;
        }

        try {
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode choices = response.get(CHOICES);
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get(MESSAGE);
            if (message == null) {
                return null;
            }

            // 检查是否有tool_calls
            JsonNode toolCalls = message.get(TOOL_CALLS);
            if (toolCalls != null && toolCalls.isArray()) {
                for (JsonNode toolCall : toolCalls) {
                    // 从extra_content.google.thought_signature中提取
                    JsonNode extraContent = toolCall.get("extra_content");
                    if (extraContent != null) {
                        JsonNode google = extraContent.get("google");
                        if (google != null) {
                            JsonNode signature = google.get(THOUGHT_SIGNATURE);
                            if (signature != null) {
                                String sigValue = signature.asText();
                                log.info("从响应中提取到thought_signature: {}", sigValue);
                                return sigValue;
                            }
                        }
                    }
                    
                    // 兼容旧格式:直接在tool_call中
                    JsonNode signature = toolCall.get(THOUGHT_SIGNATURE);
                    if (signature != null) {
                        String sigValue = signature.asText();
                        log.info("从响应中提取到thought_signature(旧格式): {}", sigValue);
                        return sigValue;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("解析响应体失败,可能是非JSON格式: {}", e.getMessage());
            return null;
        }
    }
}
