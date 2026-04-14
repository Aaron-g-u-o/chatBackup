package com.abin.mallchat.common.guild.domain.vo.response;

import lombok.Data;

@Data
public class VoiceMemberManageResult {
    private Boolean success;
    private String message;
    private ChannelMemberResp member;
}
