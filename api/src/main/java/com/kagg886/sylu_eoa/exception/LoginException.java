package com.kagg886.sylu_eoa.exception;

import lombok.Getter;

import java.io.IOException;

/**
 * 登录过程中出现的异常
 *
 * @author kagg886
 * @date 2023/9/3 17:28
 **/
public class LoginException extends IOException {
    public LoginException() {

    }

    public LoginException(String msg) {
        super(msg);
    }

    public static class CookieInitFailed extends LoginException {

    }

    public static class CookieOutOfDate extends LoginException {
        public CookieOutOfDate(String msg) {
            super(msg);
        }

        public CookieOutOfDate() {

        }
    }

    @Getter
    public static class NeedCaptcha extends LoginException {

        private final byte[] image;

        public NeedCaptcha(byte[] image) {
            super("需要验证码");
            this.image = image;
        }
    }
}
