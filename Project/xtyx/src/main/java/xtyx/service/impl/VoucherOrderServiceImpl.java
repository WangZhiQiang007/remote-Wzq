package xtyx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.springframework.aop.framework.AopContext;
import xtyx.dto.Result;
import xtyx.entity.VoucherOrder;
import xtyx.mapper.VoucherOrderMapper;
import xtyx.service.ISeckillVoucherService;
import xtyx.service.IVoucherOrderService;
import xtyx.service.IVoucherService;
import xtyx.utils.RedisIdWorker;
import xtyx.utils.UserHolder;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
	@Resource
	private ISeckillVoucherService seckillVoucherService;
	@Resource
	private RedisIdWorker redisIdWorker;
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private RedissonClient redissonClient;
	
	private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
	static {
		SECKILL_SCRIPT = new DefaultRedisScript<>();
		SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
		SECKILL_SCRIPT.setResultType(Long.class);
	}
	private static final DefaultRedisScript<Long> VOUCCHER_SCRIPT;
	static {
		VOUCCHER_SCRIPT = new DefaultRedisScript<>();
		VOUCCHER_SCRIPT.setLocation(new ClassPathResource("voucher.lua"));
		VOUCCHER_SCRIPT.setResultType(Long.class);
	}
	private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
	private static final ExecutorService SECKILL_ORDER_EXCUETOR = Executors.newSingleThreadExecutor();
	
	//在当前类加载完成后执行
	@PostConstruct
	private  void init(){
	SECKILL_ORDER_EXCUETOR.submit(new VoucherOrderHandler());
	}
	private class VoucherOrderHandler implements Runnable{
		
		@Override
		public void run() {
			while (true){
				try {
					//获取队列中的订单信息
					VoucherOrder take = orderTasks.take();
				} catch (Exception e) {
					log.error("处理订单异常了",e);
				}
			}
		}
	}
	private void handleVoucherOrder(VoucherOrder voucherOrder){
	 	//获取用户
		Long userId = voucherOrder.getUserId();
		//创建锁对象
		RLock lock = redissonClient.getLock("lock:order"+userId);
		//获取锁
		boolean isLock = lock.tryLock();
		if (!isLock){
			log.error("不允许重复下单");
			return;
		}
		try{
		//获取代理对象
			proxy.createVoucherOrder(voucherOrder);
		}finally {
			lock.unlock();
		}
	
	}
	private IVoucherOrderService proxy;
	@Resource
	private IVoucherService VoucherService;
	@Override
	public Result seckillVoucher(Long voucherId) {
		Long userId = UserHolder.getUser().getId();
		//查询是否是普通优惠券，是的话不做一人一单处理
		if (VoucherService.getById(voucherId).getType() == 2){
			Long result = stringRedisTemplate.execute(
					VOUCCHER_SCRIPT,
					Collections.emptyList(),
					voucherId.toString(), userId.toString()
			);
			int value = result.intValue();
			//2. 判断结果是否为0
			if (value != 0 ){
				//2.1.不为零 没有购买资格
				return Result.fail("库存不足了");
			}
			//2.2 有购买资格 下单消息存到阻塞队列redis
			VoucherOrder voucherOrder = new VoucherOrder();
			long orderId = redisIdWorker.nextId("order");
			voucherOrder.setVoucherId(voucherId);
			voucherOrder.setUserId(userId);
			voucherOrder.setId(orderId);
			orderTasks.add(voucherOrder);
			return Result.ok(orderId);
		}
		
		//1.执行lua脚本
		Long result = stringRedisTemplate.execute(
				SECKILL_SCRIPT,
				Collections.emptyList(),
				voucherId.toString(), userId.toString()
		);
		int value = result.intValue();
		//2. 判断结果是否为0
		if (value != 0 ){
			//2.1.不为零 没有购买资格
			return Result.fail(value == 1 ?"库存不足了" :"您已经购买过了" );
		}
		
		
		//2.2 有购买资格 下单消息存到阻塞队列redis
		VoucherOrder voucherOrder = new VoucherOrder();
		long orderId = redisIdWorker.nextId("order");
		voucherOrder.setVoucherId(voucherId);
		voucherOrder.setUserId(userId);
		voucherOrder.setId(orderId);
		orderTasks.add(voucherOrder);
//		//获取代理对象
//		proxy = (IVoucherOrderService) AopContext.currentProxy();
		//3 返回订单id
		return Result.ok(orderId);
	}
	
	/*
		public Result seckillVoucher(Long voucherId) {
	
			// 查询优惠券
			SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
			//		判断是否开始或者结束
			if (voucher.getBeginTime().isAfter(LocalDateTime.now()))
				return Result.fail("秒杀尚未开始");
			if (voucher.getEndTime().isBefore(LocalDateTime.now())){
				return Result.fail("秒杀已经结束");
			}
			//		判断是否有库存
			if (voucher.getStock() < 1){
				return Result.fail("库存不足");
			}
			//		加悲观锁，当用户Id值一样锁就一样
			Long userId = UserHolder.getUser().getId();
	//		解决事务未提交就释放锁的问题
	//		发现锁失效问题，只能锁当前进程（当前jvm有当前的锁监视器），有多个并行进程时（其他的jvm有自己的锁监视器，这时就会出现锁不能跨进程的问题造成锁失效）可以使用分布式锁解决问题
			/*synchronized (userId.toString().intern()) {
	//			直接调用会造成事务失效，需要使用代理对象进行调用
				IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
				return proxy.createVoucherOrder(voucherId);
			}
	//		使用分布式锁
	//		创建锁对象
	//		SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
			RLock simpleRedisLock = redissonClient.getLock("order:" + userId);
			//		获取锁
			boolean isLock = simpleRedisLock.tryLock();
	//		判断获取锁是否成功
			if (!isLock){
				return Result.fail("不允许重复下单");
			}
			try {
				return createVoucherOrder(voucherId);
			} finally {
				simpleRedisLock.unlock();
			}
		}
	*/
	//	设计到两张表的修改，添加事务，一旦出现问题，可以及时回滚
	@Transactional
	public void createVoucherOrder(VoucherOrder voucherOrder) {
		
		
		Long userId = voucherOrder.getUserId();
		int conut = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
		if (conut > 0) {
			log.error("用户已经购买过一次");
		}
		
		//				扣减库存 查询传入的优惠券id且库存大于0 减一
		boolean success = seckillVoucherService.update()
								  .setSql("stock = stock - 1")
								  .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0).update();
		if (!success) {
			//		扣减失败
			log.error("库存不足");
		}
		save(voucherOrder);
	}
}
