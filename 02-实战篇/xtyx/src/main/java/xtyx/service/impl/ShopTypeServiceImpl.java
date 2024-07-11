package xtyx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xtyx.entity.ShopType;
import xtyx.mapper.ShopTypeMapper;
import xtyx.service.IShopTypeService;
import org.springframework.stereotype.Service;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

}
