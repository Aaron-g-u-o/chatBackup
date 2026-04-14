package com.abin.mallchat.common.guild.domain.vo.request;

import lombok.Data;

@Data
public class CreateGuildReq {
    private String name;
    private String icon;
    private String description;
    private Integer isPublic;
}
