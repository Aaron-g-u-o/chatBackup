package com.abin.mallchat.common.user.dao;

import com.abin.mallchat.common.user.domain.entity.Black;
import com.abin.mallchat.common.user.mapper.BlackMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 黑名单 服务实现类
 * </p>
 *

 */
@Service
public class BlackDao extends ServiceImpl<BlackMapper, Black> {

}
