package com.abin.mallchat.common.guild.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum ChannelTypeEnum {
    CATEGORY(0, "分组"),
    TEXT(1, "文字频道"),
    VOICE(2, "语音频道");
    
    private final Integer type;
    private final String desc;
    
    private static final Map<Integer, ChannelTypeEnum> cache;
    
    static {
        cache = Arrays.stream(ChannelTypeEnum.values())
                .collect(Collectors.toMap(ChannelTypeEnum::getType, Function.identity()));
    }
    
    public static ChannelTypeEnum of(Integer type) {
        return cache.get(type);
    }
}
