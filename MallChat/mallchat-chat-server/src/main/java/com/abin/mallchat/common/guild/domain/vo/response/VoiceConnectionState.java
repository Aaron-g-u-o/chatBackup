package com.abin.mallchat.common.guild.domain.vo.response;

import lombok.Data;

@Data
public class VoiceConnectionState {
    private Long uid;
    private String connectionState;
    private Integer latency;
    private String quality;
    private Long lastUpdateTime;
}
