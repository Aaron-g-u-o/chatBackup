package com.abin.mallchat.common.voice.domain.vo.ws;

import lombok.Data;

@Data
public class WSVoiceSignal {
    private Integer type;
    private Long voiceRoomId;
    private Long fromUid;
    private Long targetUid;
    private String sdp;
    private String candidate;
}
