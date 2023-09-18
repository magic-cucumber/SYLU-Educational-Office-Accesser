package com.kagg886.sylu_eoa;

import com.kagg886.sylu_eoa.model.LoginAuthorization;
import com.kagg886.sylu_eoa.model.RSAPublicKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ISyluTest {
    @BeforeAll
    static void initSyluAPI() {
        ISylu.setInstance(new ISyluImpl());
        System.out.println("ISylu init success");

    }

    @Test
    void testCookie() {
        Assertions.assertDoesNotThrow(() -> {
            SyluUser user = SyluUser.createUser("2203050528", "JSESSIONID=38DE6A3379EF6CDE786DDD8E36FBF704");

            Assertions.assertFalse(user.isCookieOutOfDate());
        });
    }

    @Test
    void initCookie() {
        Assertions.assertDoesNotThrow(() -> {
            System.out.println(ISylu.getInstance().initAuthorization());
        });
    }

    @Test
    void login() {

        LoginAuthorization authorization = ISylu.getInstance().initAuthorization();
        RSAPublicKey RSAKey = ISylu.getInstance().initRSAPublicKey(authorization.getCookie());

        authorization.setUser("2203050528");
        authorization.setPublicKey(RSAKey);
        authorization.setPassWord("Aa12345678");

        Assertions.assertDoesNotThrow(() -> {
            ISylu.getInstance().login(authorization);
        });
    }

    @Test
    void logout() throws IOException {
        String cookie = "114514";

        ISylu.getInstance().logout(cookie);
    }

}