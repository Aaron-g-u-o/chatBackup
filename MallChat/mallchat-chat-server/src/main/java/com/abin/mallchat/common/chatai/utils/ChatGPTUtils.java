package com.abin.mallchat.common.chatai.utils;

import com.abin.mallchat.common.chatai.domain.ChatGPTMsg;
import com.abin.mallchat.common.common.exception.BusinessException;
import com.abin.mallchat.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ChatGPTUtils {

    private static final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    private static final String URL = "https://api.openai.com/v1/chat/completions";

    private String model = "gpt-3.5-turbo";

    private final Map<String, String> headers;
    /**
     * 超时30秒
     */
    private Integer timeout = -1;
    /**
     * 参数用于指定生成文本的最大长度。
     * 它表示生成的文本中最多包含多少个 token。一个 token 可以是一个单词、一个标点符号或一个空格。
     */
    private int maxTokens = 2048;
    /**
     * 用于控制生成文本的多样性。
     * 较高的温度会导致更多的随机性和多样性，但可能会降低生成文本的质量。默认值为 1，建议在 0.7 到 1.3 之间调整。
     */
    private Object temperature = 1;
    /**
     * 用于控制生成文本的多样性。
     * 它会根据概率选择最高的几个单词，而不是选择概率最高的单词。默认值为 1，建议在 0.7 到 0.9 之间调整。
     */
    private Object topP = 0.9;
    /**
     * 用于控制生成文本中重复单词的数量。
     * 较高的惩罚值会导致更少的重复单词，但可能会降低生成文本的流畅性。默认值为 0，建议在 0 到 2 之间调整。
     */
    private Object frequencyPenalty = 0.0;
    /**
     * 用于控制生成文本中出现特定单词的数量。
     * 较高的惩罚值会导致更少的特定单词，但可能会降低生成文本的流畅性。默认值为 0，建议在 0 到 2 之间调整。
     */
    private Object presencePenalty = 0.6;

    /**
     * 提示词
     */
    private List<ChatGPTMsg> messages;
//    private List<ChatGPTMsg> prompt;

    private String proxyUrl;

    public ChatGPTUtils(String key) {
        HashMap<String, String> _headers_ = new HashMap<>();
        _headers_.put("Content-Type", "application/json");
        if (StringUtils.isBlank(key)) {
            throw new BusinessException("openAi key is blank");
        }
        _headers_.put("Authorization", "Bearer " + key);
        this.headers = _headers_;
    }

    public static ChatGPTUtils create(String key) {
        return new ChatGPTUtils(key);
    }

    @SneakyThrows
    public static String parseText(Response response) {
        return parseText(response.body().string());
    }


    public static String parseText(String body) {
        if (StringUtils.isBlank(body)) {
            log.warn("parseText: body is blank");
            return "闹脾气了，等会再试试吧~";
        }
        log.info("parseText: body length = {}, preview = {}", body.length(), 
            body.length() > 500 ? body.substring(0, 500) + "..." : body);
        
        StringBuilder sb = new StringBuilder();
        try {
            if (!body.contains("data:")) {
                log.info("Response is not SSE format, trying to parse as regular JSON");
                try {
                    JsonNode root = JsonUtils.toJsonNode(body);
                    if (root.has("error")) {
                        String errorMsg = root.path("error").path("message").asText();
                        log.error("API returned error: {}", errorMsg);
                        return "API报错: " + errorMsg;
                    }
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode first = choices.get(0);
                        JsonNode message = first.path("message");
                        if (!message.isMissingNode()) {
                            String content = message.path("content").asText();
                            if (StringUtils.isNotBlank(content)) {
                                return content;
                            }
                        }
                    }
                    log.warn("Non-SSE response parse failed, body: {}", body);
                } catch (Exception e) {
                    log.warn("Failed to parse as regular JSON: {}", e.getMessage());
                }
                return "闹脾气了，等会再试试吧~";
            }
            
            String[] parts = body.split("data:");
            log.info("SSE format detected, parts count: {}", parts.length);
            
            for (String part : parts) {
                String x = StringUtils.trimToEmpty(part);
                if (StringUtils.isBlank(x)) {
                    continue;
                }
                if ("[DONE]".equals(x)) {
                    continue;
                }
                JsonNode root;
                try {
                    root = JsonUtils.toJsonNode(x);
                } catch (Exception ex) {
                    log.debug("skip invalid json chunk: {}", x);
                    continue;
                }
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.size() == 0) {
                    log.debug("no choices array in chunk: {}", x);
                    continue;
                }
                JsonNode first = choices.get(0);
                if (first == null || first.isMissingNode()) {
                    log.debug("empty first choice in chunk: {}", x);
                    continue;
                }
                JsonNode delta = first.path("delta");
                if (delta == null || delta.isMissingNode()) {
                    log.debug("no delta node in first choice: {}", x);
                    continue;
                }
                JsonNode content = delta.findValue("content");
                if (content == null || content.isNull()) {
                    continue;
                }
                String text = content.asText();
                if (StringUtils.isNotBlank(text)) {
                    sb.append(text);
                }
            }
            
            String result = sb.toString();
            log.info("parseText result length: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("parseText error e:", e);
            return "闹脾气了，等会再试试吧~";
        }
    }

    public ChatGPTUtils model(String model) {
        this.model = model;
        return this;
    }

    public ChatGPTUtils timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ChatGPTUtils maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public ChatGPTUtils temperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public ChatGPTUtils topP(int topP) {
        this.topP = topP;
        return this;
    }

    public ChatGPTUtils frequencyPenalty(int frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
    }

    public ChatGPTUtils presencePenalty(int presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
    }

    public ChatGPTUtils message(List<ChatGPTMsg> messages) {
        this.messages = messages;
        return this;
    }

    public ChatGPTUtils proxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
        return this;
    }

    public Response send() throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("model", model);
        paramMap.put("messages", messages);
        paramMap.put("max_tokens", maxTokens);
        paramMap.put("temperature", temperature);
        paramMap.put("top_p", topP);
        paramMap.put("frequency_penalty", frequencyPenalty);
        paramMap.put("presence_penalty", presencePenalty);
        paramMap.put("stream", true);

        String requestUrl = StringUtils.isNotBlank(proxyUrl) ? proxyUrl : URL;
        String requestBody = JsonUtils.toStr(paramMap);
        log.info("paramMap >>> " + requestBody);
        log.info("Sending request to URL: {}", requestUrl);
        
        Request request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", headers.get("Authorization"))
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                .build();
        
        try {
            Response response = okHttpClient.newCall(request).execute();
            log.info("Response received - code: {}, message: {}", response.code(), response.message());
            return response;
        } catch (IOException e) {
            log.error("API request failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public static Integer countTokens(String messages) {
        return encoding.countTokens(messages);
    }

    public static Integer countTokens(List<ChatGPTMsg> msg) {
        return countTokens(JsonUtils.toStr(msg));
    }


}
