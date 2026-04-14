package com.abin.mallchat.common.voice.domain.vo.ws;

import com.abin.mallchat.common.voice.domain.vo.response.VoiceMemberResp;
import lombok.Data;

import java.util.List;

@Data
public class WSVoiceRoomUpdate {
    private Long voiceRoomId;
    private String action;
    private VoiceMemberResp member;
    private List<VoiceMemberResp> members;
}
