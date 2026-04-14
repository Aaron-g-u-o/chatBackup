package com.abin.mallchat.common.guild.domain.vo.response;

import lombok.Data;

@Data
public class ChannelMemberResp {
    private Long uid;
    private String name;
    private String avatar;
    private Integer muted;
    private Integer deafened;
    private Integer speaking;
    private Integer volume;
}
