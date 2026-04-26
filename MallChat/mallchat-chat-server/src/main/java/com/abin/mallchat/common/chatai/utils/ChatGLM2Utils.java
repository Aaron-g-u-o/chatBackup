package com.abin.mallchat.common.chatai.utils;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChatGLM2Utils {

    private final Map<String, String> headers;
    private Integer timeout = 60000;
    private String url;
    private String apiKey;
    private String model = "glm-4-flash";
    private String prompt;
    private List<Map<String, String>> messages;
    private Integer maxTokens = 2048;
    private Double temperature = 0.95;

    public ChatGLM2Utils() {
        HashMap<String, String> _headers_ = new HashMap<>();
        _headers_.put("Content-Type", "application/json");
        this.headers = _headers_;
        this.messages = new ArrayList<>();
    }

    public static ChatGLM2Utils create() {
        return new ChatGLM2Utils();
    }

    public ChatGLM2Utils url(String url) {
        this.url = url;
        return this;
    }

    public ChatGLM2Utils apiKey(String apiKey) {
        this.apiKey = apiKey;
        this.headers.put("Authorization", "Bearer " + apiKey);
        return this;
    }

    public ChatGLM2Utils model(String model) {
        this.model = model;
        return this;
    }

    public ChatGLM2Utils timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ChatGLM2Utils prompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public ChatGLM2Utils maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public ChatGLM2Utils temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public ChatGLM2Utils addMessage(String role, String content) {
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

        if (messages != null && !messages.isEmpty()) {
            for (Map<String, String> msg : messages) {
                JSONObject msgObj = new JSONObject();
                msgObj.set("role", msg.get("role"));
                msgObj.set("content", msg.get("content"));
                messagesArray.add(msgObj);
            }
        } else if (prompt != null) {
            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messagesArray.add(userMsg);
        }

        param.set("messages", messagesArray);
        param.set("max_tokens", maxTokens);
        param.set("temperature", temperature);

        log.info("GLM API Request - URL: {}, Model: {}", url, model);
        log.debug("GLM API Request - Headers: {}, Body: {}", headers, param);

        return HttpUtil.createPost(url)
                .addHeaders(headers)
                .body(param.toString())
                .timeout(timeout)
                .execute();
    }

    public static String parseText(String body) {
        log.debug("GLM API Response - Body: {}", body);

        try {
            JSONObject jsonObj = JSONUtil.parseObj(body);

            if (jsonObj.containsKey("error")) {
                String errorMsg = jsonObj.getStr("error");
                log.error("GLM API Error: {}", errorMsg);
                return "抱歉，AI服务暂时不可用，请稍后再试~";
            }

            JSONArray choices = jsonObj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("GLM API Response - No choices: {}", body);
                return "AI好像走神了，再说一次吧~";
            }

            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            if (message == null) {
                log.error("GLM API Response - No message: {}", body);
                return "AI好像走神了，再说一次吧~";
            }

            String content = message.getStr("content");
            return content != null ? content : "AI好像走神了，再说一次吧~";

        } catch (Exception e) {
            log.error("GLM API Response Parse Error: {}", e.getMessage());
            return "AI服务出了点小问题，请稍后再试~";
        }
    }

    public static String parseText(HttpResponse response) {
        if (response == null) {
            return "网络连接失败，请检查网络~";
        }
        return parseText(response.body());
    }

    public static void main(String[] args) {
        String testApiKey = "your-api-key-here";

        HttpResponse send = ChatGLM2Utils
                .create()
                .url("https://open.bigmodel.cn/api/paas/v4/chat/completions")
                .apiKey(testApiKey)
                .model("glm-4-flash")
                .timeout(60000)
                .prompt("你好，请介绍一下你自己")
                .maxTokens(1024)
                .temperature(0.95)
                .send();

        System.out.println("Response: " + parseText(send));
    }
}
