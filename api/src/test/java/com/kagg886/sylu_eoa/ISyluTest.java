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
            ISylu.getInstance().assertLogin("JSESSIONID=4933F0B1D6621BDD0CE32626D1060837");
        });
    }

    @Test
    void initCookie() {
        Assertions.assertDoesNotThrow(() -> {
            System.out.println(ISylu.getInstance().initCookie());
        });
    }

    @Test
    void initRSAPublicKey() {
        Assertions.assertDoesNotThrow(() -> {
            RSAPublicKey key = ISylu.getInstance().initRSAPublicKey(ISylu.getInstance().initCookie());
            System.out.println(key);
        });
    }

    @Test
    void login() {
        String cookie = ISylu.getInstance().initCookie();
        RSAPublicKey RSAKey = ISylu.getInstance().initRSAPublicKey(cookie);

        LoginAuthorization authorization = new LoginAuthorization();
        authorization.setUser("2203050528");
        authorization.setCookie(cookie);
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