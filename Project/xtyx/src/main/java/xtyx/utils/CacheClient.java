package xtyx.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import xtyx.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
  @Resource
  private StringRedisTemplate stringRedisTemplate;
  private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
  public void set(String key, Object value, Long time, TimeUnit unit){
	stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
  }
  
  public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
	//设置逻辑过期
	RedisData redisData = new RedisData();
	redisData.setData(value);
	redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
//	写入redis
	stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
 }
  public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Long time, TimeUnit unit, Function<ID,R> dbFallback){
  	String key = keyPrefix + id;
	  String json = stringRedisTemplate.opsForValue().get(key);
	  if (StrUtil.isNotBlank(json)) {
		  Shop shop = com.alibaba.fastjson2.JSONObject.parseObject(json, Shop.class);
		  System.out.println(shop);
		  return JSONUtil.toBean(json, type);
	  }
	  if (json != null) {
		  return null;
	  }
//	  不存在，数据库中查询
	  R r = dbFallback.apply(id);
	  if (r == null) {
		  stringRedisTemplate.opsForValue().set(key,"", RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
		  return null;
	  }
	  stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),time,unit);
	  return r;
  }
  public <R,ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
  	String key = keyPrefix + id;
	  String json = stringRedisTemplate.opsForValue().get(key);
	  if (StrUtil.isNotBlank(json)) {
		  Shop shop = JSONUtil.parseObj(json).get("data", Shop.class);
//		  Shop shop = com.alibaba.fastjson2.JSONObject.parseObject(json, Shop.class);
		  System.out.println(shop);
		  return JSONUtil.toBean(json, type);
	  }
	  if (json != null) {
		  return null;
	  }
	  Boolean trylock = trylock(RedisConstants.LOCK_KEY + id);
	  R r = null;
	  
		  try {
			  Boolean isLock = trylock(RedisConstants.LOCK_KEY + id);
			  if (!isLock){
			  	Thread.sleep(50);
			  return queryWithMutex(keyPrefix,id,type,dbFallback,time,unit);
			  }
			  r = dbFallback.apply(id);
			  if (r == null){
				  stringRedisTemplate.opsForValue().set(key,"", RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
				  return null;
			  }
			  this.set(key,r,time,unit);
			  
		  } catch (Exception e) {
			  throw new RuntimeException(e);
		  }finally {
			  unLock(RedisConstants.LOCK_KEY + id);
		  }
		  return r;
  }
  
  public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
	  String key = keyPrefix + id;
	  // 1.从redis查询商铺缓存
	  String json = stringRedisTemplate.opsForValue().get(key);
	  // 2.判断是否存在
	  if (StrUtil.isBlank(json)) {
		  // 3.存在，直接返回
		  if (json != null){
			  return null;
		  }
		  Boolean trylock = trylock(RedisConstants.LOCK_KEY + id);
		  R r = null;
		  
		  try {
			  Boolean isLock = trylock(RedisConstants.LOCK_KEY + id);
			  if (!isLock){
				  Thread.sleep(50);
				  return queryWithMutex(keyPrefix,id,type,dbFallback,time,unit);
			  }
			  r = dbFallback.apply(id);
			  if (r == null){
				  stringRedisTemplate.opsForValue().set(key,"", RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
				  return null;
			  }
			  this.set(key,r,time,unit);
			  
		  } catch (Exception e) {
			  throw new RuntimeException(e);
		  }finally {
			  unLock(RedisConstants.LOCK_KEY + id);
		  }
		  return r;
	  }
	  // 4.命中，需要先把json反序列化为对象
	  RedisData redisData = JSONUtil.toBean(json, RedisData.class);
	  
	  R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
	  LocalDateTime expireTime = redisData.getExpireTime();
	  // 5.判断是否过期
	  if (expireTime.isAfter(LocalDateTime.now())) {
		  // 5.1.未过期，直接返回店铺信息
		  return r;
	  }
	  // 5.2.已过期，需要缓存重建
	  // 6.缓存重建
	  // 6.1.获取互斥锁
	  String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
	  boolean isLock = trylock(lockKey);
	  // 6.2.判断是否获取锁成功
	  if (isLock) {
		  // 6.3.成功，开启独立线程，实现缓存重建
		  CACHE_REBUILD_EXECUTOR.submit(() -> {
			  try {
				  // 查询数据库
				  R newR = dbFallback.apply(id);
				  // 重建缓存
				  this.setWithLogicalExpire(key, newR, time, unit);
			  } catch (Exception e) {
				  throw new RuntimeException(e);
			  } finally {
				  // 释放锁
				  unLock(lockKey);
			  }
		  });
	  }
	  // 6.4.返回过期的商铺信息
	  return r;
  }
	
	private Boolean trylock(String key) {
	Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.SECONDS);
	return Boolean.TRUE.equals(flag);
  }
  private void unLock(String key) {
	stringRedisTemplate.delete(key);
  }
}