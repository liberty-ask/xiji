package com.xiji.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xiji.config.CustomConfig;
import com.xiji.entity.dto.response.VoiceTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 语音记账AI服务
 * 使用智谱AI的glm-4-flash模型解析语音文本
 * @author liberty
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoiceTransactionAiService {

    private final CustomConfig customConfig;

    /**
     * 使用智谱AI解析语音文本
     * 
     * @param text 语音转文字后的文本
     * @param categories 系统可用的分类列表
     * @return 解析后的交易信息
     */
    public VoiceTransactionResponse parseVoiceText(String text, List<String> categories) {
        try {
            // 获取当前日期作为默认值
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(categories, today);
            
            // 调用智谱AI API
            String aiResponse = callZhipuAi(systemPrompt, text);
            
            // 解析AI返回的JSON
            VoiceTransactionResponse result = parseAiResponse(aiResponse, text, today);
            
            return result;
        } catch (Exception e) {
            log.error("AI解析语音文本失败: {}", text, e);
            throw new RuntimeException("语音记账失败");
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(List<String> categories, String today) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的记账助手。请从用户输入的文本中提取记账信息。\n\n");
        prompt.append("要求：\n");
        prompt.append("1. 提取交易金额（必须是数字，支持小数）\n");
        prompt.append("2. 判断交易类型：支出（type=1）或收入（type=0）\n");
        prompt.append("3. 匹配分类名称，必须从以下分类列表中选择一个最匹配的：");
        prompt.append(String.join("、", categories));
        prompt.append("\n");
        prompt.append("4. 提取交易日期，格式为yyyy-MM-dd。如果文本中没有明确提到日期，请使用：");
        prompt.append(today);
        prompt.append("\n");
        prompt.append("5. 提取备注信息（可以是原始文本或提取的关键信息）\n\n");
        prompt.append("请严格按照以下JSON格式返回，不要添加任何其他文字说明：\n");
        prompt.append("{\n");
        prompt.append("  \"type\": 1,  // 0表示收入，1表示支出\n");
        prompt.append("  \"amount\": 10.00,  // 金额（数字）\n");
        prompt.append("  \"category\": \"食品\",  // 分类名称（必须从提供的分类列表中选择）\n");
        prompt.append("  \"date\": \"2026-01-19\",  // 日期（yyyy-MM-dd格式）\n");
        prompt.append("  \"note\": \"买苹果\"  // 备注\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * 调用智谱AI API
     * 使用官方SDK: ai.z.openapi:zai-sdk:0.3.0
     * 参考文档: https://docs.bigmodel.cn/cn/guide/develop/java/introduction
     */
    private String callZhipuAi(String systemPrompt, String userText) {
        try {
            String apiKey = customConfig.getZhipuAi().getApiKey();
            String model = customConfig.getZhipuAi().getModel();
            
            // 创建客户端（根据官方文档）
            ZhipuAiClient client = ZhipuAiClient.builder().ofZHIPU()
                    .apiKey(apiKey)
                    .build();
            
            // 创建聊天请求（根据官方文档）
            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(model) // 使用glm-4-flash
                    .messages(Arrays.asList(
                            // 系统消息
                            ChatMessage.builder()
                                    .role(ChatMessageRole.SYSTEM.value())
                                    .content(systemPrompt)
                                    .build(),
                            // 用户消息
                            ChatMessage.builder()
                                    .role(ChatMessageRole.USER.value())
                                    .content(userText)
                                    .build()
                    ))
                    .stream(false) // 非流式响应
                    .build();
            
            // 发送请求
            ChatCompletionResponse response = client.chat().createChatCompletion(request);
            
            // 检查响应是否成功
            if (!response.isSuccess()) {
                throw new RuntimeException("智谱AI调用失败: " + response.getMsg());
            }
            
            // 提取响应内容
            ChatMessage assistantMessage = response.getData().getChoices().get(0).getMessage();
            Object contentObj = assistantMessage.getContent();
            
            // 将Object类型转换为String
            String content;
            if (contentObj == null) {
                throw new RuntimeException("AI返回内容为空");
            } else if (contentObj instanceof String) {
                content = (String) contentObj;
            } else {
                // 如果不是String类型，尝试转换为String
                content = contentObj.toString();
            }
            
            if (content.isEmpty()) {
                throw new RuntimeException("AI返回内容为空");
            }
            
            return content;
        } catch (Exception e) {
            log.error("调用智谱AI失败", e);
            throw new RuntimeException("调用智谱AI失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析AI返回的JSON响应
     */
    private VoiceTransactionResponse parseAiResponse(String aiResponse, String originalText, String defaultDate) {
        try {
            // 清理AI可能返回的Markdown代码块标记
            String cleanedJson = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
            
            // 如果响应被包裹在代码块中，提取JSON部分
            if (cleanedJson.startsWith("{")) {
                int endIndex = cleanedJson.lastIndexOf("}");
                if (endIndex > 0) {
                    cleanedJson = cleanedJson.substring(0, endIndex + 1);
                }
            }
            
            // 解析JSON
            JsonObject jsonObject = JsonParser.parseString(cleanedJson).getAsJsonObject();
            
            VoiceTransactionResponse response = new VoiceTransactionResponse();
            
            // 解析类型
            if (jsonObject.has("type")) {
                int type = jsonObject.get("type").getAsInt();
                response.setType(type);
            } else {
                // 默认支出
                response.setType(1);
            }
            
            // 解析金额
            if (jsonObject.has("amount")) {
                BigDecimal amount = jsonObject.get("amount").getAsBigDecimal();
                response.setAmount(amount);
            } else {
                throw new RuntimeException("AI未返回金额信息");
            }
            
            // 解析分类
            if (jsonObject.has("category")) {
                String category = jsonObject.get("category").getAsString();
                response.setCategory(category);
            } else {
                throw new RuntimeException("AI未返回分类信息");
            }
            
            // 解析日期
            if (jsonObject.has("date") && !jsonObject.get("date").isJsonNull()) {
                String dateStr = jsonObject.get("date").getAsString();
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    response.setDate(date);
                } catch (Exception e) {
                    log.warn("AI返回的日期格式不正确: {}, 使用默认日期", dateStr);
                    response.setDate(LocalDate.parse(defaultDate));
                }
            } else {
                // 使用默认日期（今天）
                response.setDate(LocalDate.parse(defaultDate));
            }
            
            // 解析备注
            if (jsonObject.has("note") && !jsonObject.get("note").isJsonNull()) {
                String note = jsonObject.get("note").getAsString();
                response.setNote(note);
            } else {
                // 使用原始文本作为备注
                response.setNote(originalText);
            }
            
            return response;
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", aiResponse, e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }
}

