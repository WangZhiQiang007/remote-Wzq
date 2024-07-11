package xtyx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xtyx.dto.LoginFormDTO;
import xtyx.dto.Result;
import xtyx.dto.UserDTO;
import xtyx.entity.User;
import xtyx.mapper.UserMapper;
import xtyx.service.IUserService;
import xtyx.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static xtyx.utils.RedisConstants.*;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Override
	public Result sendCode(String phone, HttpSession session) {
		if (RegexUtils.isPhoneInvalid(phone)){
			return Result.fail("手机号格式错误");
		}else {
			String code = RandomUtil.randomNumbers(6);
			stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+ phone,code,LOGIN_CODE_TTL, TimeUnit.SECONDS);
			
		log.debug("发送验证码成功，验证码为：{}",code);
		}
		return Result.ok(stringRedisTemplate.opsForValue().get(phone));
	}

	@Override
	public Result login(LoginFormDTO loginForm, HttpSession session) {
			String phone = loginForm.getPhone();
			if (RegexUtils.isPhoneInvalid(phone)){
				return Result.fail("手机号格式错误");
			}else{
				if (RegexUtils.isCodeInvalid(loginForm.getCode())){
					return Result.fail("验证码无效");
				}else{
					String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+ phone);
					if (!loginForm.getCode().equals(code) || code == null){
						return Result.fail("验证码错误");
					}
				}
			}
		User user = query().eq("phone", phone).one();
			if (user == null){
				user = new User();
				user.setPhone(phone);
				user.setNickName("user_"+RandomUtil.randomString(10));
				save(user); //保存用户
			}
			String token = UUID.randomUUID().toString();
			UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//			存到redis的数据只保留想要的，对用户关键信息隐藏
			stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,BeanUtil.beanToMap(userDTO, new HashMap<>(),
					CopyOptions.create().setIgnoreNullValue(true)
							.setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())));
			stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
		return Result.ok(token);
	}
	
	@Override
	public Result logout(HttpServletRequest request) {
		String token = request.getHeader("authorization");
		Boolean delete = stringRedisTemplate.delete(LOGIN_USER_KEY + token);
		if (BooleanUtil.isTrue(delete)){
			return Result.ok("登出成功");
		}
		return Result.fail("登出失败");
		
	}
	
	@Override
	public Result login2(LoginFormDTO loginForm, HttpSession session) {
		if (RegexUtils.isPhoneInvalid(loginForm.getPhone())){
			return Result.fail("手机号格式错误");
		}
		//判断数据库里是否存有手机号，有的话证明用户登陆过，可以直接登录否则直接登陆失败
		QueryWrapper<User> phone = new QueryWrapper<User>().eq("phone", loginForm.getPhone());
		if (getOne(phone) != null){
			//登陆成功
			User user = query().eq("phone", loginForm.getPhone()).one();
			String token = UUID.randomUUID().toString();
			UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
			//存到redis的数据只保留想要的，对用户关键信息隐藏
			stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,BeanUtil.beanToMap(userDTO, new HashMap<>(),
					CopyOptions.create().setIgnoreNullValue(true)
							.setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())));
			stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
			return Result.ok(token);
		}else {
			return Result.fail("登陆失败,请先使用验证码登录");
		}
		
	
	}
}
