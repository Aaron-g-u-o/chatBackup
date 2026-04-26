package com.abin.mallchat.common.discovery.domain.vo.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateInterestTagsReq {
    private List<Long> tagIds;
}
