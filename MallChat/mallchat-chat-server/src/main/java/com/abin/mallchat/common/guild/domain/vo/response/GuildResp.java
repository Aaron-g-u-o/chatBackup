package com.abin.mallchat.common.guild.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class GuildResp {
    private Long id;
    private String name;
    private String icon;
    private String description;
    private Long ownerUid;
    private Integer memberCount;
    private Integer isPublic;
    private String inviteCode;
    private List<ChannelResp> channels;
}
