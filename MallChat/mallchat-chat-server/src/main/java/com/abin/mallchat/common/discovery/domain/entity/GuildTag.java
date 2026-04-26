package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("guild_tag")
public class GuildTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String category;
    
    private Double weight;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
