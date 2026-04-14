package com.abin.mallchat.common.guild.domain.vo.request;

import lombok.Data;

@Data
public class CreateChannelReq {
    private Long guildId;
    private Long parentId;
    private String name;
    private Integer type;
    private String topic;
    private Integer maxUsers;
}
