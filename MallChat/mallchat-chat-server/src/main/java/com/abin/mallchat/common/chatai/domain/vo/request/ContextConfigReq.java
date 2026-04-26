package com.abin.mallchat.common.chatai.domain.vo.request;

import lombok.Data;

@Data
public class ContextConfigReq {
    private Integer aiType;
    private Integer maxContextWindow;
    private Integer enableIntentTracking;
    private Integer enableRelevanceFilter;
    private Double relevanceThreshold;
    private Long customPromptId;
    private Double diversityRatio;
}
