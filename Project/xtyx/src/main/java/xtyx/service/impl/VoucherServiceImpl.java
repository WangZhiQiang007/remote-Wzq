package xtyx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xtyx.dto.Result;
import xtyx.entity.SeckillVoucher;
import xtyx.entity.Voucher;
import xtyx.mapper.VoucherMapper;
import xtyx.service.ISeckillVoucherService;
import xtyx.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static xtyx.utils.RedisConstants.SECKILL_STOCK_KEY;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VoucherMapper VoucherMapper;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        QueryWrapper<Voucher> id = new QueryWrapper<Voucher>().eq("shop_id", shopId);
        List<Voucher> vouchers = VoucherMapper.selectList(id);
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
//        保存秒杀优惠券信息到redis
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }
}
