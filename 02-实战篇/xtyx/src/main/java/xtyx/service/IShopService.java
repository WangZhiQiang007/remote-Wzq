package xtyx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xtyx.dto.Result;
import xtyx.entity.Shop;


public interface IShopService extends IService<Shop> {

Result queryById(Long id);

Result update(Shop shop);
}
