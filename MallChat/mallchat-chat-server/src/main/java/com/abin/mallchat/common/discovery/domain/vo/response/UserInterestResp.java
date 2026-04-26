package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class UserInterestResp {
    private Integer privacyLevel;
    private Double diversityPreference;
    private List<UserInterestTagResp> interestTags;
}
