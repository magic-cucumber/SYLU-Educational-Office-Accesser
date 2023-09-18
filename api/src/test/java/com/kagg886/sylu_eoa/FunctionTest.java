package com.kagg886.sylu_eoa;

import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 功能测试
 *
 * @author kagg886
 * @date 2023/9/4 11:29
 **/
public class FunctionTest {
    static String cookie;
    static String id;

    @BeforeAll
    static void login() throws IOException {
        id = "2203050528";
        ISylu.setInstance(new ISyluImpl());
        System.out.println("ISylu init success");


        String cookie = ISylu.getInstance().initCookie();
        RSAPublicKey RSAKey = ISylu.getInstance().initRSAPublicKey(cookie);

        LoginAuthorization authorization = new LoginAuthorization();
        authorization.setUser(id);
        authorization.setCookie(cookie);
        authorization.setPublicKey(RSAKey);
        authorization.setPassWord("Baleitem103");

        FunctionTest.cookie = ISylu.getInstance().login(authorization);
    }

    @Test
    void testUserProfile() throws IOException {
        Profile o = ISylu.getInstance().getUserProfile(id, cookie);
        System.out.println(o);
    }

    @Test
    void testSchoolCalender() throws IOException {
        SchoolCalender calender = ISylu.getInstance().getSchoolCalender(id, cookie);
        System.out.println(calender);
    }

    @Test
    void testExamResult() throws IOException {
        Term term = new Term("2022-2023", "1");
        List<ExamResult> results = ISylu.getInstance().getExamList(id, cookie, term);

        System.out.println(results);

        System.out.println(JSON.toJSONString(results));
    }

    @Test
    void testExamInfo() throws IOException {
        Term term = new Term("2022-2023", "1");
        List<ExamResult> results = ISylu.getInstance().getExamList(id, cookie, term);

        System.out.println(results);

        List<List<String>> lists = ISylu.getInstance().getExamInfo(id, cookie, results.get(0));
        System.out.println(lists);
    }


    @Test
    void testClassTable() throws IOException {
        Term t = ISylu.getInstance().getSchoolCalender(id, cookie).getCurrentTerm();
        List<ClassUnit> units = ISylu.getInstance().getClassTable(cookie, id, t);
        System.out.println(units);
    }

    @Test
    void getGPAScores() throws IOException {
        Map<String,List<GPAScore>> res = ISylu.getInstance().getGPAScores(cookie,id);
        System.out.println(res);
    }
}
