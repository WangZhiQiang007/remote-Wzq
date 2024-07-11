package xtyx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xtyx.dto.Result;
import xtyx.entity.Voucher;


public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
