package com.abin.mallchat.common.discovery.domain.vo.request;

import lombok.Data;

@Data
public class DismissReq {
    private Long guildId;
    private String reason;
}
