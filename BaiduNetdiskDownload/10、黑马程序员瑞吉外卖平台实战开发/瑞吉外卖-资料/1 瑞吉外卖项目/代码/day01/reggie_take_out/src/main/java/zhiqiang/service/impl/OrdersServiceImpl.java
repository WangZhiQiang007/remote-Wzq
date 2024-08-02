package zhiqiang.service.impl;

import zhiqiang.entity.Orders;
import zhiqiang.mapper.OrdersMapper;
import zhiqiang.service.IOrdersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author WangZhiQiang
 * @since 2024-08-02
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {

}
