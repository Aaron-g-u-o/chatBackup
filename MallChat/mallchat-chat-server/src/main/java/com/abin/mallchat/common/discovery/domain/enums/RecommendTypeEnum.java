package com.abin.mallchat.common.discovery.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecommendTypeEnum {
    PERSONALIZED(0, "个性化推荐"),
    POPULAR(1, "热门推荐"),
    NEWEST(2, "最新推荐"),
    TRENDING(3, "趋势推荐");
    
    private final Integer type;
    private final String desc;
    
    public static RecommendTypeEnum of(Integer type) {
        for (RecommendTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return PERSONALIZED;
    }
}
