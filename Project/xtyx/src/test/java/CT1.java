import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import xtyx.XTYX_Application;
import xtyx.dto.Result;
import xtyx.entity.Shop;
import xtyx.entity.User;
import xtyx.entity.Voucher;
import xtyx.mapper.UserMapper;
import xtyx.service.IShopService;
import xtyx.service.IVoucherOrderService;
import xtyx.service.IVoucherService;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest(classes = XTYX_Application.class)
@Service
public class CT1 extends ServiceImpl<UserMapper,User> implements IService<User>  {
	
	@Autowired
	IShopService shopService;
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Test
	void testHyperLogLog(){
		String[] values = new String[1000];
		int j = 0;
		for (int i = 0; i < 1000000; i++) {
			j = i % 1000;
			values[j] = "user_"+ i;
			if (j == 999){
				//发送到redis
				stringRedisTemplate.opsForHyperLogLog().add("hl2",values);
			}
		}
		Long hl2 = stringRedisTemplate.opsForHyperLogLog().size("hl2");
		System.out.println("count= "+hl2);
		
	}
	@Test
	void testGeo(){
		GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search("shop:geo:1", new Circle(120.151954, 30.32497, 1000));
		List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
		if (list == null){
			log.error("查询不到数据");
		}else{
			//遍历list集合
			list.forEach(geoResult -> {
				//获取店铺id
				String shopId = geoResult.getContent().getName();
				//获取店铺
				Shop shop = shopService.getById(Long.valueOf(shopId));
				//获取店铺信息
				System.out.println(shop);
			});
		}
	}
	@Test
	void loodShopData(){
		List<Shop> list = shopService.list();
		Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
		for (Map.Entry<Long, List<Shop>> entry : map.entrySet()){
			Long typeId = entry.getKey();
			String key = "shop:geo:" + typeId;
			List<Shop> shops = entry.getValue();
			List<RedisGeoCommands.GeoLocation<String>> locations= new ArrayList<>(shops.size());
			for (Shop shop : shops){
				locations.add(new RedisGeoCommands.GeoLocation<>(
						shop.getId().toString(),
						new Point(shop.getX(),shop.getY())
				));
			}
			stringRedisTemplate.opsForGeo().add(key,locations);
		}
	}
	
	@Test
	public void test() {
		User user = new User();
		user.setPassword("13422223242");
		user.setNickName("扬帆起航-05");
		user.setPhone("13422223242");
		user.setCreateBy("Wzq02");
		lambdaUpdate()
				.eq(User::getPhone,"13323232323")
				.set(User::getPassword,user.getPassword())
				.update();
		update()
				.eq("phone","13323232323")
				.set("password",user.getPassword())
				.update();
//		通过new对象进行修改
		update()
				.eq("phone","13323232323")
				.update(user);
	}
	
	@Override
	public boolean saveBatch(Collection<User> entityList, int batchSize) {
		return false;
	}
	
	@Override
	public boolean saveOrUpdateBatch(Collection<User> entityList, int batchSize) {
		return false;
	}
	
	@Override
	public boolean updateBatchById(Collection<User> entityList, int batchSize) {
		return false;
	}
	
	@Override
	public boolean saveOrUpdate(User entity) {
		return false;
	}
	
	@Override
	public User getOne(Wrapper<User> queryWrapper, boolean throwEx) {
		return null;
	}
	
	@Override
	public Map<String, Object> getMap(Wrapper<User> queryWrapper) {
		return null;
	}
	
	@Override
	public <V> V getObj(Wrapper<User> queryWrapper, Function<? super Object, V> mapper) {
		return null;
	}
	
	
	
	@Override
	public Class<User> getEntityClass() {
		return null;
	}
}
