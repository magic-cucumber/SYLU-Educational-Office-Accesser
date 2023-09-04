package com.kagg886.sylu_eoa.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.Arrays;

/**
 * 管理http
 *
 * @author kagg886
 * @date 2023/9/3 17:40
 **/
public class HTTPUtil {

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
}
