package com.abin.mallchat.common.chatai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chatai.chatglm2")
public class ChatGLM2Properties {

    private boolean use = false;

    private String apiKey;

    private String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private String model = "glm-4-flash";

    private Long AIUserId;

    private Long minute = 1L;

    private Integer timeout = 60000;

    private Integer maxTokens = 2048;

    private Double temperature = 0.95;

    private Integer limit = 100;

}
