package com.abin.mallchat.common.discovery.domain.vo.request;

import lombok.Data;

@Data
public class UpdatePrivacyReq {
    private Integer privacyLevel;
    private Double diversityPreference;
}
