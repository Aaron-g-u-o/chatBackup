package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

@Data
public class RecommendationMetricsResp {
    private Long totalRecommendations;
    private Long totalClicks;
    private Long totalJoins;
    private Double clickThroughRate;
    private Double conversionRate;
    private Double avgRelevanceScore;
}
