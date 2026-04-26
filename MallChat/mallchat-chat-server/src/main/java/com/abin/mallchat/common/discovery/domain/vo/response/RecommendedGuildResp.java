package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class RecommendedGuildResp {
    private Long id;
    private String name;
    private String icon;
    private String description;
    private String category;
    private List<String> tags;
    private Integer memberCount;
    private String language;
    private Integer activityLevel;
    private Boolean isJoined;
    private Double relevanceScore;
    private Integer recommendSource;
    private String recommendSourceDesc;
    private Integer onlineCount;
}
