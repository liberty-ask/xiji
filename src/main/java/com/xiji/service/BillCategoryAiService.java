package com.xiji.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xiji.config.CustomConfig;
import com.xiji.entity.domain.Category;
import com.xiji.entity.dto.response.BillCategoryAiResult;
import com.xiji.parser.model.BillTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 账单分类AI服务
 * 使用智谱AI的glm-4-flash模型对账单交易进行智能归类
 * @author liberty
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillCategoryAiService {

    private final CustomConfig customConfig;
    
    // 默认批量大小
    private static final int DEFAULT_BATCH_SIZE = 20;
    // AI调用超时时间（秒）
    private static final int AI_CALL_TIMEOUT = 60;
    // 提示词缓存，key为分类列表的哈希值，value为系统提示词
    private final Map<String, String> promptCache = new ConcurrentReferenceHashMap<>();
    
    /**
     * 批量归类账单交易
     * 
     * @param transactions 需要归类的交易列表
     * @param incomeCategories 收入分类列表
     * @param expenseCategories 支出分类列表
     * @return 归类结果列表（与输入列表顺序对应）
     */
    public List<BillCategoryAiResult> categorizeTransactions(
            List<BillTransaction> transactions,
            List<Category> incomeCategories,
            List<Category> expenseCategories) {
        
        if (transactions == null || transactions.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取批量大小配置（可以从配置中读取，这里先用默认值）
        int batchSize = DEFAULT_BATCH_SIZE;
        
        // 构建系统提示词（带缓存）
        String systemPrompt = getSystemPrompt(incomeCategories, expenseCategories);
        
        // 计算总批次数
        int totalBatches = (int) Math.ceil((double) transactions.size() / batchSize);
        log.info("开始AI归类，总交易数：{}，批次大小：{}，总批次数：{}", 
                transactions.size(), batchSize, totalBatches);
        
        // 异步处理所有批次
        List<CompletableFuture<List<BillCategoryAiResult>>> futures = new ArrayList<>();
        
        for (int i = 0; i < transactions.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, transactions.size());
            List<BillTransaction> batch = transactions.subList(i, endIndex);
            final int startIndex = i;
            
            // 异步处理单个批次
            CompletableFuture<List<BillCategoryAiResult>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return categorizeBatch(batch, systemPrompt, startIndex);
                } catch (Exception e) {
                    log.error("批量归类失败，批次起始索引：{}", startIndex, e);
                    // 失败时使用默认分类
                    List<BillCategoryAiResult> defaultResults = new ArrayList<>();
                    for (int j = 0; j < batch.size(); j++) {
                        BillCategoryAiResult defaultResult = new BillCategoryAiResult();
                        defaultResult.setIndex(startIndex + j);
                        defaultResult.setCategory(null); // 返回null，让调用方使用默认分类
                        defaultResult.setConfidence(0.0);
                        defaultResults.add(defaultResult);
                    }
                    return defaultResults;
                }
            });
            futures.add(future);
        }
        
        // 等待所有批次完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        
        try {
            // 设置超时时间
            allOf.get(AI_CALL_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("AI归类超时，已处理部分批次");
            // 取消所有未完成的任务
            futures.forEach(future -> future.cancel(true));
        } catch (Exception e) {
            log.error("等待AI归类结果失败", e);
        }
        
        // 收集所有结果
        List<BillCategoryAiResult> results = new ArrayList<>();
        for (CompletableFuture<List<BillCategoryAiResult>> future : futures) {
            if (future.isDone() && !future.isCancelled()) {
                try {
                    results.addAll(future.get());
                } catch (Exception e) {
                    log.error("获取AI归类结果失败", e);
                }
            }
        }
        
        // 按索引排序，确保结果顺序正确
        results.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        
        log.info("AI归类完成，总交易数：{}，成功处理：{}", transactions.size(), results.size());
        
        return results;
    }

    /**
     * 批量归类一批交易
     */
    private List<BillCategoryAiResult> categorizeBatch(
            List<BillTransaction> batch,
            String systemPrompt,
            int startIndex) {
        
        // 构建用户消息（包含所有交易信息）
        String userMessage = buildUserMessage(batch);
        
        // 调用AI
        String aiResponse = callZhipuAi(systemPrompt, userMessage);
        
        // 解析AI返回的JSON数组
        List<BillCategoryAiResult> results = parseBatchResponse(aiResponse, startIndex);
        
        // 确保返回结果的数量与输入批次数量一致
        if (results.size() != batch.size()) {
            log.warn("AI返回结果数量({})与输入批次数量({})不一致，startIndex：{}", 
                    results.size(), batch.size(), startIndex);
            // 补充缺失的结果
            while (results.size() < batch.size()) {
                int index = startIndex + results.size();
                BillCategoryAiResult defaultResult = new BillCategoryAiResult();
                defaultResult.setIndex(index);
                defaultResult.setCategory(null);
                defaultResult.setConfidence(0.0);
                results.add(defaultResult);
            }
        }
        
        return results;
    }
    
    /**
     * 从缓存中获取或构建系统提示词
     */
    private String getSystemPrompt(List<Category> incomeCategories, List<Category> expenseCategories) {
        // 生成缓存key
        String incomeKey = incomeCategories.stream()
                .map(Category::getName)
                .sorted()
                .collect(Collectors.joining(","));
        String expenseKey = expenseCategories.stream()
                .map(Category::getName)
                .sorted()
                .collect(Collectors.joining(","));
        String cacheKey = incomeKey + "|" + expenseKey;
        
        // 从缓存获取
        if (promptCache.containsKey(cacheKey)) {
            return promptCache.get(cacheKey);
        }
        
        // 构建提示词
        String systemPrompt = buildSystemPrompt(incomeCategories, expenseCategories);
        
        // 存入缓存
        promptCache.put(cacheKey, systemPrompt);
        
        return systemPrompt;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(List<Category> incomeCategories, List<Category> expenseCategories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个账单分类助手。请根据交易信息，从以下系统分类列表中选择最合适的分类。\n\n");
        
        // 收入分类列表
        if (incomeCategories != null && !incomeCategories.isEmpty()) {
            prompt.append("系统收入分类列表：");
            prompt.append(incomeCategories.stream()
                    .map(Category::getName)
                    .collect(Collectors.joining("、")));
            prompt.append("\n\n");
        }
        
        // 支出分类列表
        if (expenseCategories != null && !expenseCategories.isEmpty()) {
            prompt.append("系统支出分类列表：");
            prompt.append(expenseCategories.stream()
                    .map(Category::getName)
                    .collect(Collectors.joining("、")));
            prompt.append("\n\n");
        }
        
        prompt.append("要求：\n");
        prompt.append("1. 如果原始分类在系统分类列表中，优先使用原始分类\n");
        prompt.append("2. 如果原始分类不在系统分类列表中，根据交易描述、原始分类等信息智能匹配最合适的分类\n");
        prompt.append("3. 必须从系统分类列表中选择，不能创建新分类\n");
        prompt.append("4. 返回JSON数组格式，每个元素对应一条交易\n\n");
        prompt.append("返回格式：\n");
        prompt.append("[\n");
        prompt.append("  {\"index\": 0, \"category\": \"分类名称\", \"confidence\": 0.95},\n");
        prompt.append("  {\"index\": 1, \"category\": \"分类名称\", \"confidence\": 0.88},\n");
        prompt.append("  ...\n");
        prompt.append("]\n");
        
        return prompt.toString();
    }

    /**
     * 构建用户消息（包含所有交易信息）
     */
    private String buildUserMessage(List<BillTransaction> transactions) {
        StringBuilder message = new StringBuilder();
        message.append("请为以下交易记录进行分类：\n\n");
        
        for (int i = 0; i < transactions.size(); i++) {
            BillTransaction t = transactions.get(i);
            message.append("交易").append(i).append("：\n");
            message.append("- 索引：").append(i).append("\n");
            message.append("- 原始分类：").append(t.getCategory() != null ? t.getCategory() : "无").append("\n");
            message.append("- 描述：").append(t.getDescription() != null ? t.getDescription() : "无").append("\n");
            message.append("\n");
        }
        
        message.append("请返回JSON数组，按照索引顺序返回归类结果。");
        
        return message.toString();
    }

    /**
     * 调用智谱AI API
     */
    private String callZhipuAi(String systemPrompt, String userMessage) {
        try {
            String apiKey = customConfig.getZhipuAi().getApiKey();
            String model = customConfig.getZhipuAi().getModel();
            
            // 创建客户端
            ZhipuAiClient client = ZhipuAiClient.builder().ofZHIPU()
                    .apiKey(apiKey)
                    .build();
            
            // 创建聊天请求
            ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            ChatMessage.builder()
                                    .role(ChatMessageRole.SYSTEM.value())
                                    .content(systemPrompt)
                                    .build(),
                            ChatMessage.builder()
                                    .role(ChatMessageRole.USER.value())
                                    .content(userMessage)
                                    .build()
                    ))
                    .stream(false)
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
     * 解析批量响应
     */
    private List<BillCategoryAiResult> parseBatchResponse(String aiResponse, int startIndex) {
        try {
            // 清理AI可能返回的Markdown代码块标记
            String cleanedJson = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
            
            // 提取JSON数组部分
            if (cleanedJson.startsWith("[")) {
                int endIndex = cleanedJson.lastIndexOf("]");
                if (endIndex > 0) {
                    cleanedJson = cleanedJson.substring(0, endIndex + 1);
                }
            }
            
            // 解析JSON数组
            JsonArray jsonArray = JsonParser.parseString(cleanedJson).getAsJsonArray();
            
            List<BillCategoryAiResult> results = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                
                BillCategoryAiResult result = new BillCategoryAiResult();
                
                // 解析索引（需要加上startIndex偏移）
                if (jsonObject.has("index")) {
                    int index = jsonObject.get("index").getAsInt();
                    result.setIndex(startIndex + index);
                }
                
                // 解析分类
                if (jsonObject.has("category") && !jsonObject.get("category").isJsonNull()) {
                    String category = jsonObject.get("category").getAsString();
                    result.setCategory(category);
                }
                
                // 解析置信度
                if (jsonObject.has("confidence") && !jsonObject.get("confidence").isJsonNull()) {
                    double confidence = jsonObject.get("confidence").getAsDouble();
                    result.setConfidence(confidence);
                } else {
                    result.setConfidence(0.8); // 默认置信度
                }
                
                results.add(result);
            }
            
            return results;
        } catch (Exception e) {
            log.error("解析AI批量响应失败: {}", aiResponse, e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }
}

