package com.abin.mallchat.common.chatai.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class QianfanUtils {

    private static final String V2_BASE_URL = "https://qianfan.baidubce.com/v2/chat/completions";
    
    private String apiKey;
    private String model = "ernie-3.5-8k";
    private Integer timeout = 60000;
    private Integer maxTokens = 2048;
    private Double temperature = 0.95;
    private List<Map<String, String>> messages = new ArrayList<>();

    public QianfanUtils() {
    }

    public static QianfanUtils create() {
        return new QianfanUtils();
    }

    public QianfanUtils apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public QianfanUtils secretKey(String secretKey) {
        return this;
    }

    public QianfanUtils model(String model) {
        this.model = model;
        return this;
    }

    public QianfanUtils timeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public QianfanUtils maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public QianfanUtils temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public QianfanUtils addMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        this.messages.add(message);
        return this;
    }

    public HttpResponse send() {
        JSONObject param = new JSONObject();
        param.set("model", model);
        
        JSONArray messagesArray = new JSONArray();
        for (Map<String, String> msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.set("role", msg.get("role"));
            msgObj.set("content", msg.get("content"));
            messagesArray.add(msgObj);
        }
        
        param.set("messages", messagesArray);
        param.set("max_tokens", maxTokens);
        param.set("temperature", temperature);
        
        log.info("Qianfan V2 API Request - Model: {}, MessageCount: {}", model, messages.size());
        log.debug("Qianfan V2 API Request - Body: {}", param);
        
        return HttpRequest.post(V2_BASE_URL)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .body(param.toString())
            .timeout(timeout)
            .execute();
    }

    public static String parseText(HttpResponse response) {
        if (response == null) {
            return "网络连接失败，请检查网络~";
        }
        return parseText(response.body());
    }

    public static String parseText(String body) {
        log.debug("Qianfan V2 API Response: {}", body);
        
        if (body == null || body.isEmpty()) {
            return "AI好像走神了，再说一次吧~";
        }
        
        try {
            JSONObject json = JSONUtil.parseObj(body);
            
            if (json.containsKey("error")) {
                JSONObject error = json.getJSONObject("error");
                String errorMsg = error != null ? error.getStr("message", "未知错误") : "未知错误";
                String errorCode = error != null ? error.getStr("code", "") : "";
                log.error("Qianfan V2 API Error: {} - {}", errorCode, errorMsg);
                return "AI服务暂时不可用: " + errorMsg;
            }
            
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("Qianfan V2 API Response - No choices: {}", body);
                return "AI好像走神了，再说一次吧~";
            }
            
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            if (message == null) {
                log.error("Qianfan V2 API Response - No message: {}", body);
                return "AI好像走神了，再说一次吧~";
            }
            
            String content = message.getStr("content");
            if (content != null && !content.isEmpty()) {
                log.info("Qianfan V2 Response length: {}", content.length());
                return content;
            }
            
            log.warn("No content in Qianfan V2 response: {}", body);
            return "AI好像走神了，再说一次吧~";
            
        } catch (Exception e) {
            log.error("Error parsing Qianfan V2 response: {}", e.getMessage(), e);
            return "AI服务出了点小问题，请稍后再试~";
        }
    }
}
