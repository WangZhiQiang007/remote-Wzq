package xtyx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xtyx.dto.Result;
import xtyx.entity.VoucherOrder;


public interface IVoucherOrderService extends IService<VoucherOrder> {
	
	Result seckillVoucher(Long voucherId);
	
	void createVoucherOrder(VoucherOrder voucherOrder);
}
