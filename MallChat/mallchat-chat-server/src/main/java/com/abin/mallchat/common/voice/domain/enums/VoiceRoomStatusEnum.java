package com.abin.mallchat.common.voice.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum VoiceRoomStatusEnum {
    ACTIVE(1, "活跃"),
    CLOSED(0, "已关闭");
    
    private final Integer status;
    private final String desc;
    
    private static final Map<Integer, VoiceRoomStatusEnum> cache;
    
    static {
        cache = Arrays.stream(VoiceRoomStatusEnum.values())
                .collect(Collectors.toMap(VoiceRoomStatusEnum::getStatus, Function.identity()));
    }
    
    public static VoiceRoomStatusEnum of(Integer status) {
        return cache.get(status);
    }
}
