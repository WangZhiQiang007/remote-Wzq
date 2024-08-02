package xtyx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xtyx.dto.Result;
import xtyx.entity.Follow;


public interface IFollowService extends IService<Follow> {
	
	Result isFollow(Long followUserId);
	
	Result follow(Long followUserId, Boolean isFollow);
	
	Result followCommons(Long id);
}
