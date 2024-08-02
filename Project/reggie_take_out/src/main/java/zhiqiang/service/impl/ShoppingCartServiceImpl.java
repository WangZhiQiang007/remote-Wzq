package zhiqiang.service.impl;

import zhiqiang.entity.ShoppingCart;
import zhiqiang.mapper.ShoppingCartMapper;
import zhiqiang.service.IShoppingCartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 购物车 服务实现类
 * </p>
 *
 * @author WangZhiQiang
 * @since 2024-08-02
 */
@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements IShoppingCartService {

}
