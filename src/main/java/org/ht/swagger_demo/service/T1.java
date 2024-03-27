package org.ht.swagger_demo.service;

import org.springframework.stereotype.Service;

@Service
public class T1 {

}
    /**
private <T> QueryWrapper<T> 构建查询条件(QueryWrapper<T> queryWrapper, List<条件> conditions) {
        for (条件 condition : conditions) {
            // 根据条件对象构建查询条件
            String fieldName = condition.getFieldName();
            Object value = condition.getValue();
//             根据字段类型和条件类型动态添加查询条件
                        switch (condition.getConditionType()) {
                            case EQ: // 等于
                                queryWrapper.eq(fieldName, value);
                                break;
                            case LIKE: // 模糊查询
                                queryWrapper.like(fieldName, value.toString());
                                break;
                            // 可以添加更多的条件类型
                            default:
                                throw new IllegalArgumentException("不支持的条件类型");
                        }
                    }
                    return queryWrapper;


}
     public <T> List<T> 查询列表(Class<T> clazz, List<条件> conditions) {
     QueryWrapper<T> queryWrapper = new QueryWrapper<>();
     queryWrapper = 构建查询条件(queryWrapper, conditions);
     return mapper.selectList(queryWrapper);
     }

     @Autowired
     private 通用Service commonService;

     public List<实体类> 查询用户列表(String name, Integer age) {
     List<条件> conditions = new ArrayList<>();
     conditions.add(new 条件("属性", xxx, 条件类型.EQ));
     conditions.add(new 条件("属性", xxx, 条件类型.EQ));
     return commonService.查询列表(User.class, conditions);
     }
*/