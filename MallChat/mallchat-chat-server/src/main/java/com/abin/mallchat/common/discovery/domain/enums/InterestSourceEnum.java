package com.abin.mallchat.common.discovery.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InterestSourceEnum {
    GUILD_JOIN(0, "加入服务器"),
    ACTIVITY(1, "活跃行为"),
    MESSAGE_CONTENT(2, "消息内容"),
    MANUAL(3, "手动选择");
    
    private final Integer type;
    private final String desc;
}
