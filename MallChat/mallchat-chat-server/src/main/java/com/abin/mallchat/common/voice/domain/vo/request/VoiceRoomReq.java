package com.abin.mallchat.common.voice.domain.vo.request;

import lombok.Data;

@Data
public class VoiceRoomReq {
    private Long roomId;
    private String name;
    private Integer maxUsers;
}
