package com.abin.mallchat.common.discovery.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PrivacyLevelEnum {
    FULL_PERSONALIZED(0, "完全个性化"),
    JOIN_HISTORY_ONLY(1, "仅基于加入记录"),
    POPULAR_ONLY(2, "仅热门推荐");
    
    private final Integer level;
    private final String desc;
}
