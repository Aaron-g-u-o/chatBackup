package com.abin.mallchat.common.guild.domain.vo.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateGuildReq {
    private String name;
    private String icon;
    private String description;
    private Integer isPublic;
    private String category;
    private List<String> tags;
    private String language;
}
