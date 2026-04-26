package com.abin.mallchat.common.chatai.domain.vo.response;

import lombok.Data;

@Data
public class AIContextConfigResp {
    private Long uid;
    private Integer aiType;
    private Integer maxContextWindow;
    private Boolean enableIntentTracking;
    private Boolean enableRelevanceFilter;
    private Double relevanceThreshold;
    private Long customPromptId;
    private String customPromptContent;
    private Double diversityRatio;
}
