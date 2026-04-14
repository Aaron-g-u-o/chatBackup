package com.abin.mallchat.common.voice.domain.vo.request;

import lombok.Data;

@Data
public class WSVoiceSignalReq {
    private Integer type;
    private Long voiceRoomId;
    private Long targetUid;
    private String sdp;
    private String candidate;
}
