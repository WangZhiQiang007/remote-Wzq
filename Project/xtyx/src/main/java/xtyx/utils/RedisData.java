package xtyx.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
//    给LocalDateTime赋初始值
    private LocalDateTime expireTime = LocalDateTime.now();
    private Object data;
}
