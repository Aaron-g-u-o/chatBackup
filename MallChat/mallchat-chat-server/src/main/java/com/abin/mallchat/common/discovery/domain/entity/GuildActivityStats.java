package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("guild_activity_stats")
public class GuildActivityStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long guildId;
    
    private Integer dailyActiveUsers;
    
    private Integer weeklyActiveUsers;
    
    private Integer dailyMessages;
    
    private Integer weeklyMessages;
    
    private Integer joinCount7d;
    
    private Double trendingScore;
    
    private Double qualityScore;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
