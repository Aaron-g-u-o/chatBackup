package com.abin.mallchat.common.discovery.service;

import com.abin.mallchat.common.discovery.domain.vo.request.UpdateInterestTagsReq;
import com.abin.mallchat.common.discovery.domain.vo.request.UpdatePrivacyReq;
import com.abin.mallchat.common.discovery.domain.vo.response.UserInterestResp;

public interface UserInterestService {
    
    void buildUserProfile(Long uid);
    
    void updateOnGuildJoin(Long uid, Long guildId);
    
    void updateOnGuildLeave(Long uid, Long guildId);
    
    void updateOnUserActivity(Long uid, Long guildId, String activityType);
    
    UserInterestResp getUserInterest(Long uid);
    
    void updatePrivacy(Long uid, UpdatePrivacyReq req);
    
    void updateManualInterestTags(Long uid, UpdateInterestTagsReq req);
    
    void refreshUserProfile(Long uid);
}
