package xtyx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xtyx.dto.LoginFormDTO;
import xtyx.dto.Result;
import xtyx.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {
	
	Result sendCode(String phone, HttpSession session);
	
	Result login(LoginFormDTO loginForm, HttpSession session);
	
	Result logout(HttpServletRequest request);
	
	Result login2(LoginFormDTO loginForm, HttpSession session);
}
