package com.abin.mallchat.common.voice.domain.vo.response;

import lombok.Data;

@Data
public class VoiceMemberResp {
    private Long uid;
    private String name;
    private String avatar;
    private Integer muted;
    private Integer deafened;
    private Integer speaking;
}
