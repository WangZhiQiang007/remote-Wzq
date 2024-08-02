package zhiqiang.service.impl;

import zhiqiang.entity.OrderDetail;
import zhiqiang.mapper.OrderDetailMapper;
import zhiqiang.service.IOrderDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单明细表 服务实现类
 * </p>
 *
 * @author WangZhiQiang
 * @since 2024-08-02
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements IOrderDetailService {

}
