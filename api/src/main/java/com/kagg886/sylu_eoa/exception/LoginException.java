package com.kagg886.sylu_eoa.exception;

import lombok.Getter;

/**
 * 登录过程中出现的异常
 *
 * @author kagg886
 * @date 2023/9/3 17:28
 **/
public class LoginException extends RuntimeException {
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

    public static class NeedCaptcha extends LoginException {

        @Getter
        private final String captchaLink;

        public NeedCaptcha(String captchaLink) {
            super("需要验证码,验证码地址:" + captchaLink);
            this.captchaLink = captchaLink;
        }
    }
}
