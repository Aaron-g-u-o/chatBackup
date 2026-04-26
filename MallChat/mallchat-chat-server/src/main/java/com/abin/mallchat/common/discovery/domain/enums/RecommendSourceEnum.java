package com.abin.mallchat.common.discovery.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecommendSourceEnum {
    COLLABORATIVE(0, "协同过滤"),
    CONTENT_BASED(1, "内容匹配"),
    SOCIAL(2, "社交关系"),
    POPULARITY(3, "热门排序");
    
    private final Integer type;
    private final String desc;
}
