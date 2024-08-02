package xtyx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import xtyx.dto.Result;
import xtyx.dto.UserDTO;
import xtyx.entity.Follow;
import xtyx.mapper.FollowMapper;
import xtyx.service.IFollowService;
import org.springframework.stereotype.Service;
import xtyx.service.IUserService;
import xtyx.utils.UserHolder;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
	
	@Override
	public Result isFollow(Long followUserId) {
		Long userId = UserHolder.getUser().getId();
		Integer count = query()
								.eq("user_id", userId)
								.eq("follow_user_id", followUserId)
								.count();
		return Result.ok (count > 0);
	}
	@Resource
	private StringRedisTemplate stringRedisTemplate ;
	@Override
	public Result follow(Long followUserId, Boolean isFollow) {
		
		// 1.获取登录用户
		Long userId = UserHolder.getUser().getId();
		// 1.判断要进行关注还是取关
		if (isFollow) {
			// 2.关注，新增数据
			Follow follow = new Follow();
			follow.setUserId(userId);
			follow.setFollowUserId(followUserId);
			boolean isSuccess = save(follow);
			if (isSuccess){
				stringRedisTemplate.opsForSet().add("follows:" + userId, followUserId.toString());
			}
		} else {
			// 3.取关，删除数据
			boolean isSuccess = remove(
//					lambdaQuery()
//							.eq(Follow::getUserId, userId)
//							.eq(Follow::getFollowUserId, followUserId));
					new QueryWrapper<Follow>()
							.eq("user_id", userId)
							.eq("follow_user_id", followUserId));
			if (isSuccess) {
				stringRedisTemplate.opsForSet().remove("follows:" + userId, followUserId.toString());
			}
		}
		
		
		return Result.ok();
	}
	
	@Resource
	private IUserService userServiceser;
	@Override
	public Result followCommons(Long id) {
		Long userId = UserHolder.getUser().getId();
		//目标用户关注列表
		String key = "follows:" + id;
		//当前用户关注列表
		String key2 = "follows:" + userId;
		Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
		if (intersect == null){
			return Result.ok(Collections.emptyList());
		}
		List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
		List<UserDTO> users = userServiceser.listByIds(ids)
										.stream()
										.map(user -> BeanUtil.copyProperties(user, UserDTO.class))
										.collect(Collectors.toList());
		return Result.ok(users);
		
	}
}
