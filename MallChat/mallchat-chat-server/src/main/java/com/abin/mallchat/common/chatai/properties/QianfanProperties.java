package com.abin.mallchat.common.chatai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chatai.qianfan")
public class QianfanProperties {

    private boolean use = false;

    private String apiKey;

    private String secretKey;

    private String model = "ernie_speed";

    private Long AIUserId;

    private Integer timeout = 60000;

    private Integer maxTokens = 2048;

    private Double temperature = 0.95;

    private Integer limit = 100;

    private String accessTokenUrl = "https://aip.baidubce.com/oauth/2.0/token";
    
    private String chatUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/";
}
