package com.abin.mallchat.common.discovery.domain.vo.request;

import lombok.Data;

@Data
public class DiscoveryReq {
    private Integer recommendType = 0;
    private String category;
    private Integer minMembers;
    private Integer maxMembers;
    private String language;
    private Integer activityLevel;
    private Integer page = 1;
    private Integer pageSize = 20;
}
