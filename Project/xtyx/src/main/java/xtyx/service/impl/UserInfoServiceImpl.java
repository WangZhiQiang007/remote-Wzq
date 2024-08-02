package xtyx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xtyx.entity.UserInfo;
import xtyx.mapper.UserInfoMapper;
import xtyx.service.IUserInfoService;
import org.springframework.stereotype.Service;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
