package com.kagg886.sylu_eoa;

import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;

public class SyluUser {
    @Getter
    private String userID;
    private String cookie;

    private String getCookie() {
        return cookie;
    }

    private void setCookie(String cookie) {
        this.cookie = cookie;
    }

    //响应式更新需要
    private void setUserID(String userID) {
        this.userID = userID;
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
    public YearAndSemestersPicker getPicker() {
        return ISylu.getInstance().getPicker(cookie, userID);
    }

    @SneakyThrows
    public List<ExamResult> getExamListByTerm(Term term) {
        return ISylu.getInstance().getExamList(userID, cookie, term);
    }

    @SneakyThrows
    public Profile getProfile() {
        return ISylu.getInstance().getUserProfile(userID, cookie);
    }

    @SneakyThrows
    public SchoolCalender getSchoolCalender() {
        return ISylu.getInstance().getSchoolCalender(userID, cookie);
    }

    public static SyluUser createUser(String id) {
        SyluUser user = new SyluUser();
        user.setUserID(id);
        return user;
    }

    @SneakyThrows
    public void loginByPwd(String pwd) {
        loginByPwdAndCaptcha(pwd, null);
    }

    @SneakyThrows
    public void logout() {
        ISylu.getInstance().logout(cookie);
    }

    @SneakyThrows
    public void loginByPwdAndCaptcha(String pwd, String captcha) {
        LoginAuthorization auth = ISylu.getInstance().initAuthorization();
        RSAPublicKey publicKey = ISylu.getInstance().initRSAPublicKey(auth.getCookie());
        auth.setUser(userID);
        auth.setPublicKey(publicKey);
        auth.setPassWord(pwd);
        auth.setCaptcha(captcha);
        this.cookie = ISylu.getInstance().login(auth);
    }

    @SuppressWarnings("all")
    public boolean isCookieOutOfDate() {
        RuntimeException r = null;
        for (int i = 0; i < 4; i++) {
            try {
                ISylu.getInstance().assertLogin(cookie);
                return false;
            } catch (Exception e) {
                if (e instanceof LoginException.CookieOutOfDate) {
                    continue;
                }
                r = new RuntimeException(e);
            }
        }
        if (r != null) {
            throw r;
        }
        return true;
    }

    public static SyluUser createUser(String id, String cookie) {
        SyluUser user = new SyluUser();
        user.cookie = cookie;
        user.userID = id;
        return user;
    }
}
