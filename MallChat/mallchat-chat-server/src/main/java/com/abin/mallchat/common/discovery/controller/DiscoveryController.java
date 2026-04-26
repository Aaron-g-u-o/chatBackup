package com.abin.mallchat.common.discovery.controller;

import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.common.utils.RequestHolder;
import com.abin.mallchat.common.discovery.domain.vo.request.*;
import com.abin.mallchat.common.discovery.domain.vo.response.*;
import com.abin.mallchat.common.discovery.service.RecommendationService;
import com.abin.mallchat.common.discovery.service.UserInterestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/capi/discovery")
public class DiscoveryController {
    
    @Autowired
    private RecommendationService recommendationService;
    
    @Autowired
    private UserInterestService userInterestService;
    
    private Long getUid() {
        return RequestHolder.get() != null ? RequestHolder.get().getUid() : null;
    }
    
    @GetMapping("/recommend")
    public ApiResult<DiscoveryPageResp> getRecommendations(DiscoveryReq req) {
        Long uid = getUid();
        DiscoveryPageResp resp = recommendationService.getRecommendations(uid, req);
        return ApiResult.success(resp);
    }
    
    @PostMapping("/track/click")
    public ApiResult<Void> trackClick(@RequestBody TrackClickReq req) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        recommendationService.trackClick(uid, req);
        return ApiResult.success();
    }
    
    @PostMapping("/track/join/{guildId}")
    public ApiResult<Void> trackJoin(@PathVariable Long guildId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        recommendationService.trackJoin(uid, guildId);
        return ApiResult.success();
    }
    
    @PostMapping("/dismiss")
    public ApiResult<Void> dismissRecommendation(@RequestBody DismissReq req) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        recommendationService.dismissRecommendation(uid, req);
        return ApiResult.success();
    }
    
    @GetMapping("/tags")
    public ApiResult<List<TagCategoryResp>> getTagCategories() {
        DiscoveryPageResp resp = recommendationService.getRecommendations(null, new DiscoveryReq());
        return ApiResult.success(resp.getCategories());
    }
    
    @GetMapping("/interest")
    public ApiResult<UserInterestResp> getUserInterest() {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        UserInterestResp resp = userInterestService.getUserInterest(uid);
        return ApiResult.success(resp);
    }
    
    @PutMapping("/interest/privacy")
    public ApiResult<Void> updatePrivacy(@RequestBody UpdatePrivacyReq req) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        userInterestService.updatePrivacy(uid, req);
        return ApiResult.success();
    }
    
    @PutMapping("/interest/tags")
    public ApiResult<Void> updateInterestTags(@RequestBody UpdateInterestTagsReq req) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        userInterestService.updateManualInterestTags(uid, req);
        return ApiResult.success();
    }
    
    @PostMapping("/profile/build")
    public ApiResult<Void> buildUserProfile() {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        userInterestService.buildUserProfile(uid);
        return ApiResult.success();
    }
    
    @GetMapping("/metrics")
    public ApiResult<RecommendationMetricsResp> getMetrics() {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        RecommendationMetricsResp resp = recommendationService.getMetrics(uid);
        return ApiResult.success(resp);
    }
}
