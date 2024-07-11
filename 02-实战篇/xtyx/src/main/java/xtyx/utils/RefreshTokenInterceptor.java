package xtyx.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import xtyx.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static xtyx.utils.RedisConstants.LOGIN_USER_KEY;
import static xtyx.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {
	
	private StringRedisTemplate stringRedisTemplate;
	
	public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		
		//	获取请求头中的token
		String token = request.getHeader("authorization");
		if (StrUtil.isBlank(token)) {
//	        token不存在，不进行拦截
			return true;
		}
//		基于token获取redis中存的用户信息
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
		if (userMap.isEmpty()) {
			return true;
		}
//		将redis中拿到的map集合（Hash数据）转化为UserDTO对象
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

//		保存用户到ThreadLocal
		UserHolder.saveUser(userDTO);
//		刷新token有效期
		stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
		return true;
	}
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		UserHolder.removeUser();
	}
}
