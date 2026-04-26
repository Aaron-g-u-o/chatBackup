package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("recommendation_track")
public class RecommendationTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long uid;
    
    private Long guildId;
    
    private Integer recommendType;
    
    private Integer recommendSource;
    
    private Double relevanceScore;
    
    private Integer position;
    
    private Integer isClicked;
    
    private Integer isJoined;
    
    private Integer isDismissed;
    
    private String abGroup;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
