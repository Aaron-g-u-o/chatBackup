package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class DiscoveryPageResp {
    private List<RecommendedGuildResp> list;
    private Boolean isLast;
    private List<TagCategoryResp> categories;
    private UserInterestResp userInterest;
}
