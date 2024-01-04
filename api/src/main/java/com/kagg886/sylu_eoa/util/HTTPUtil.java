package com.kagg886.sylu_eoa.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * 管理http
 *
 * @author kagg886
 * @date 2023/9/3 17:40
 **/
public class HTTPUtil {

    static {
        init();
    }

    public static Connection newSession(Object... url) {
        return Jsoup.newSession()
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .url(compile(url))
                .timeout(10000);
    }

    public static String compile(Object... p) {
        StringBuilder builder = new StringBuilder("https://jxw.sylu.edu.cn");
        Arrays.stream(p).forEach(builder::append);
        return builder.toString();
    }


    static public void init() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
        } catch (KeyManagementException e) {
        }
    }
}
