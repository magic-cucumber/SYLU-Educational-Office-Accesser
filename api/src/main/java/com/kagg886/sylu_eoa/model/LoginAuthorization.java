package com.kagg886.sylu_eoa.model;

import lombok.Data;

/**
 * 登录器
 *
 * @author kagg886
 * @date 2023/9/3 17:52
 **/
@Data
public class LoginAuthorization {
    private String user;
    private String passWord;
    private String captcha;
    private String cookie;
    private RSAPublicKey publicKey;
    private String csrf;
}
