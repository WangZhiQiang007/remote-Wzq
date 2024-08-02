package xtyx.dto;

import lombok.Data;

/**
 * 封装用户登录信息
 */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
