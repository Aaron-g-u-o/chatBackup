package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_interest_tag")
public class UserInterestTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long uid;
    
    private Long tagId;
    
    private Double weight;
    
    private Integer source;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
