package com.kagg886.sylu_eoa;

import com.kagg886.sylu_eoa.model.*;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;

/**
 * 代表一个沈理user
 *
 * @author kagg886
 * @date 2023/9/4 10:19
 **/
public class SyluUser {
    private String userID;

    private String cookie;

    private SyluUser() {

    }

    @SneakyThrows
    public Map<String, List<GPAScore>> getGPAs() {
        return ISylu.getInstance().getGPAScores(cookie, userID);
    }

    @SneakyThrows
    public List<ClassUnit> getClassTableByTerm(Term term) {
        return ISylu.getInstance().getClassTable(cookie, userID, term);
    }

    @SneakyThrows
    public List<List<String>> getInfo(ExamResult result) {
        return ISylu.getInstance().getExamInfo(userID, cookie, result);
    }

    @SneakyThrows
    public Profile getProfile() {
        return ISylu.getInstance().getUserProfile(userID, cookie);
    }

    @SneakyThrows
    public SchoolCalender getSchoolCalender() {
        return ISylu.getInstance().getSchoolCalender(userID, cookie);
    }

    @SneakyThrows
    public void loginByPwdAndCaptcha(String pwd, String captcha) {
        LoginAuthorization auth = new LoginAuthorization();
        RSAPublicKey publicKey = ISylu.getInstance().initRSAPublicKey(cookie);
        auth.setUser(userID);
        auth.setCookie(cookie);
        auth.setPublicKey(publicKey);
        auth.setPassWord(pwd);
        auth.setCaptcha(captcha);
        cookie = ISylu.getInstance().login(auth);
    }

    @SneakyThrows
    public void loginByPwd(String pwd) {
        loginByPwdAndCaptcha(pwd, null);
    }

    @SneakyThrows
    public void logout() {
        ISylu.getInstance().logout(cookie);
    }

    public static SyluUser createUser(String id) {
        return createUser(id, ISylu.getInstance().initCookie());
    }

    public static SyluUser createUser(String id, String cookie) {
        SyluUser user = new SyluUser();
        user.cookie = cookie;
        user.userID = id;
        return user;
    }
}
