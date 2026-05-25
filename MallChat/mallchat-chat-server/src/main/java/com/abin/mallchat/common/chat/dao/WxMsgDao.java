package com.abin.mallchat.common.chat.dao;

import com.abin.mallchat.common.chat.domain.entity.WxMsg;
import com.abin.mallchat.common.chat.mapper.WxMsgMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 微信消息表 服务实现类
 * </p>
 */
@Service
public class WxMsgDao extends ServiceImpl<WxMsgMapper, WxMsg> {

}
