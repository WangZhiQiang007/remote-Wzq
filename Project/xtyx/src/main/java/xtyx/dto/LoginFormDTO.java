package xtyx.dto;

import lombok.Data;

/**
 * 封装用户登录信息
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
