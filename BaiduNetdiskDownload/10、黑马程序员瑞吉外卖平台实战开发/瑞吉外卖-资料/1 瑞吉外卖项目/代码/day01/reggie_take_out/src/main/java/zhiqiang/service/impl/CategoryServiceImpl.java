package zhiqiang.service.impl;

import zhiqiang.entity.Category;
import zhiqiang.mapper.CategoryMapper;
import zhiqiang.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 菜品及套餐分类 服务实现类
 * </p>
 *
 * @author WangZhiQiang
 * @since 2024-08-02
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

}
