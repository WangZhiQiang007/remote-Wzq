package xtyx.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;


@Component
public class RedisIdWorker {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	//  开始时间戳
	private static final long BEGIN_TIMESTAMP = 1640995200L;
	private static final int COUNT_BITS = 32;
	
	public long nextId(String keyPrefix) {
		LocalDateTime now = LocalDateTime.now();
//	   生成时间戳
		long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
//		生成序列号
//	  拼接一下日期，防止自增溢出
		String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
		Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

//		拼接并返回
		return timestamp << COUNT_BITS | increment;
	}
	
	public static void main(String[] args) {
		LocalDateTime time = LocalDateTime.of(2024, 5, 1, 0, 0, 0);
		long seconds = time.toEpochSecond(ZoneOffset.UTC);
		long seconds1 = 1714116238380L;
		long milliseconds = seconds1 * 1000;
		Date date = new Date(milliseconds);
		SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
		
		// 使用formatter的format方法将Date对象转换为字符串
		String formattedDate = formatter.format(date);
		
		// 打印格式化后的日期
		System.out.println(formattedDate);
		
	}
	
}

