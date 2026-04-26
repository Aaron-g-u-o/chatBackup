package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.UserDismissRecord;
import com.abin.mallchat.common.discovery.mapper.UserDismissRecordMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserDismissRecordDao extends ServiceImpl<UserDismissRecordMapper, UserDismissRecord> {
    
    public List<Long> getDismissedGuildIds(Long uid) {
        return lambdaQuery()
                .eq(UserDismissRecord::getUid, uid)
                .list()
                .stream()
                .map(UserDismissRecord::getGuildId)
                .collect(Collectors.toList());
    }
    
    public boolean isDismissed(Long uid, Long guildId) {
        return lambdaQuery()
                .eq(UserDismissRecord::getUid, uid)
                .eq(UserDismissRecord::getGuildId, guildId)
                .count() > 0;
    }
}
