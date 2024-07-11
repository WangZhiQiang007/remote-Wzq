package xtyx.config;

import xtyx.utils.LoginInterceptor;
import xtyx.utils.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MVCconfig implements WebMvcConfigurer {
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginInterceptor())
				.excludePathPatterns(
						"/user/code",
						"/user/login",
						"/blog/hot",
						"/shop/**",
						"/shop-type/**",
						"/upload/**",
						"/voucher/**",
						"/user/**"
				).order(1);
		registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
	}
}
