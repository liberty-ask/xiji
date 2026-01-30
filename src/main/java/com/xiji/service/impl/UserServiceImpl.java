package com.xiji.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.User;
import com.xiji.service.UserService;
import com.xiji.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @description 针对表【user】的数据库操作Service实现
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




