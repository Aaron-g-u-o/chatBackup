package com.abin.mallchat.common.discovery.domain.vo.response;

import lombok.Data;

@Data
public class UserInterestTagResp {
    private Long tagId;
    private String tagName;
    private String category;
    private Double weight;
    private Integer source;
}
