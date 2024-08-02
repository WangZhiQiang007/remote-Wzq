package xtyx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.connection.BitFieldSubCommands;
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
import xtyx.utils.UserHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
	
	@Override
	public Result sign() {
		//获取当前登录用户
		Long id = UserHolder.getUser().getId();
		//获取日期
		LocalDateTime now = LocalDateTime.now();
		//拼接key
		String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + id + keySuffix;
		//判断今天是本月第几天
		int dayOfMonth = now.getDayOfMonth();
		//写入redis
		stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
		return Result.ok();
	}
	
	@Override
	public Result signCount() {
		//获取当前登录用户
		Long id = UserHolder.getUser().getId();
		//获取日期
		LocalDateTime now = LocalDateTime.now();
		//拼接key
		String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + id + keySuffix;
		//判断今天是本月第几天
		int dayOfMonth = now.getDayOfMonth();
		//获取截止今天为止的所有签到记录，返回的是一个十进制数字
		List<Long> list = stringRedisTemplate.opsForValue().bitField(key,
				BitFieldSubCommands.create()
						.get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
						.valueAt(0)
		);
		if (list == null || list.isEmpty()){
			return Result.ok(0);
		}
		Long num = list.get(0);
		int count = 0;
		if (num == null || num == 0L){
			return Result.ok(0);
		}
		//循环遍历
		while (true){
			//与1做与运算
			//判断bit位是否为0
			if ((num & 1) ==0){
				//等于0 ，未签到，结束
				break;
			}else {
				//不等于0 已签到计数器+1
				count++;
			}
			//数字右移抛弃最后一位，继续计算下一位    无符号位右移(>>>)
			num >>>= 1;
		}
		return Result.ok(count);
	}
}
