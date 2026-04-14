package com.abin.mallchat.common.voice.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class VoiceRoomResp {
    private Long id;
    private String name;
    private Long roomId;
    private Long creatorUid;
    private Integer maxUsers;
    private Integer currentUserCount;
    private List<VoiceMemberResp> members;
}
