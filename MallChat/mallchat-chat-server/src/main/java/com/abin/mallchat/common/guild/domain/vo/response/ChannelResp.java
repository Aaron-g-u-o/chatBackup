package com.abin.mallchat.common.guild.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ChannelResp {
    private Long id;
    private Long guildId;
    private Long parentId;
    private String name;
    private Integer type;
    private String topic;
    private Integer position;
    private Integer maxUsers;
    private Long roomId;
    private List<ChannelMemberResp> members;
    private List<ChannelResp> children;
}
