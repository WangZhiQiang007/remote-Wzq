package zhiqiang.service.impl;

import zhiqiang.entity.User;
import zhiqiang.mapper.UserMapper;
import zhiqiang.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 *
 * @author WangZhiQiang
 * @since 2024-08-02
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
