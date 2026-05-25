package com.abin.mallchat.common.user.service;

import com.abin.mallchat.common.user.domain.vo.request.oss.UploadUrlReq;
import com.abin.mallchat.oss.domain.OssResp;

/**
 * <p>
 * oss 服务类
 * </p>
 *

 */
public interface OssService {

    /**
     * 获取临时的上传链接
     */
    OssResp getUploadUrl(Long uid, UploadUrlReq req);
}
