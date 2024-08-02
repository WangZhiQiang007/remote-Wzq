package xtyx.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import xtyx.dto.Result;
import xtyx.entity.Shop;
import xtyx.mapper.ShopMapper;
import xtyx.service.IShopService;
import xtyx.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static xtyx.utils.RedisConstants.*;
import static java.lang.Thread.sleep;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private CacheClient cacheClient;
	
	@Override
	public Result queryById(Long id) {
//	调用缓存穿透方法   this::getById(id) ==  id2 -> getById(id2)
//	  Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, CACHE_SHOP_TTL, TimeUnit.MINUTES, this::getById);
//	调用缓存击穿方法
//	  Shop shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById,CACHE_SHOP_TTL, TimeUnit.MINUTES);
//	调用利用互斥锁实现的 逻辑过期方法
		Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
		if (shop == null) {
			return Result.fail("店铺不存在");
		}
		return Result.ok(shop);
	}
	
	/**
	 * 解决缓存击穿问题
	 * 当一个频繁访问的key在缓存中失效，如缓存数据过期或被删除时，大量原本应被缓存处理的请求直接穿透缓存，
	 * 转而访问数据库或其他后端存储系统。
	 * 解决办法：
	 * 方法a>互斥锁 （利用setnx+ttl） 牺牲可用性，线程需要进行等待，有死锁风险，但是保证了数据一致性，无额外内存消耗
	 * 方法b>逻辑过期  牺牲一致性，线程读取到的是旧数据，有额外内存消耗，实现复杂。但是线程无需等待，性能较好，可用性强
	 * ps：缓存雪崩：多个key同时失效或者redis服务宕机 造成大量本该访问缓存的数据直接访问数据库，造成数据库压力过大
	 *
	 * @return
	 */
	private boolean trylock(String key) {
		Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "10", 10, TimeUnit.SECONDS);
		return Boolean.TRUE.equals(flag);
	}
	
	private void unlock(String key) {
		stringRedisTemplate.delete(key);
	}
	
	/**
	 * 互斥锁解决缓存击穿
	 *
	 * @param id 商铺id
	 * @return 返回shop
	 */
	public Shop queryWithMutex(Long id) {
		String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
		if (StrUtil.isNotBlank(shopJson)) {
			// 	商铺存在redis缓存中，直接返回shop对象，使用序列化工具把json格式转成对象
			Shop shop = JSONUtil.toBean(shopJson, Shop.class);
			return shop;
		}
//	为空值 返回空
		if (shopJson != null) {
			return null;
		}
//	实现缓存重建
//	a.获取互斥锁
		boolean isLock = trylock(LOCK_SHOP_KEY + id);

//	b.判断是否获取成功
		if (!isLock) {
			//	c.失败，则休眠并重试
			try {
				sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			queryWithMutex(id);
		}


//  d.成功，根据id查询数据库
		Shop shop = getById(id);
		if (shop == null) {
			//	将空值写入redis
			stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
			return null;
		}
		stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
		;
//  释放互斥锁
		unlock(LOCK_SHOP_KEY + id);

//  返回
		return shop;
	}
	
	@Override
	public Result update(Shop shop) {
		Long id = shop.getId();
		if (id == null) {
			return Result.fail("店铺id不能为空");
		}
//	更新数据库
		updateById(shop);
//	删除缓存
		stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
		return null;
	}
	
	@Override
	public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
		// 1.判断是否需要根据坐标查询
		if (x == null || y == null) {
			// 不需要坐标查询，按数据库查询
			Page<Shop> page = query()
									  .eq("type_id", typeId)
									  .page(new Page<>(current, 10));
			// 返回数据
			return Result.ok(page.getRecords());
		}
		// 2.计算分页参数
		int from = (current - 1) * 10; //起始条数
		int end = current * 10; //截止条数
		//查询redis按照距离排序、分页：shopId  和距离
		String key = SHOP_GEO_KEY + typeId;
		GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
																		   .search(
																				   key,
																				   GeoReference.fromCoordinate(x, y),
																				   new Distance(5000),
																				   RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
																		   );
		if(results == null){
			return Result.ok(Collections.emptyList());
		}
		List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
		if (list.size() <= from) {
			// 没有下一页了，结束
			return Result.ok(Collections.emptyList());
		}
		// 4.1.截取 from ~ end的部分
		List<Long> ids = new ArrayList<>(list.size());
		Map<String, Distance> distanceMap = new HashMap<>(list.size());
		list.stream().skip(from).forEach(result -> {
					// 4.2.获取店铺id
			String shopIdStr = result.getContent().getName();
			ids.add(Long.valueOf(shopIdStr));
			// 4.3.获取距离
			Distance distance = result.getDistance();
			distanceMap.put(shopIdStr, distance);
		});
		// 5.根据id查询Shop
		String idStr = StrUtil.join(",", ids);
		List<Shop> shops = this.query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
		for (Shop shop : shops) {
			shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
		}
		return Result.ok(shops);
	}
	
	
	/**
	 * 封装缓存穿透方法
	 * 通过写空值解决缓存穿透问题，避免多数据库中不存在的数据被建立多个线程去访问数据库造成宕机 （实现容易，易于维护，容易造成内存资源浪费）
	 * 另有一种方法也可解决缓存穿透问题 布隆过滤实现较为复杂 ，成本高，可能出现误判
	 */
	public Shop queryWithPassThrough(Long id) {
		//	1.从redis 查询shop缓存
		String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//	2.判断是否存在
		if (StrUtil.isNotBlank(shopJson)) {
			// 	3.存在直接返回
			Shop shop = JSONUtil.toBean(shopJson, Shop.class);
			return shop;
		}
//	判断是否命中空值
		if (shopJson != null) {
			return null;
		}
//	4.不存在，根据id查询数据库
		Shop shop = getById(id);
//  5.不存在 返回错误
		if (shop == null) {
			stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
			return null;
		}
//  6.存在，写入redis
		stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//  7.返回
		return shop;
	}
}
