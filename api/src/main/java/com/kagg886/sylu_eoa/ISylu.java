package com.kagg886.sylu_eoa;

import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.*;
import com.kagg886.sylu_eoa.util.HTTPUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.kagg886.sylu_eoa.util.HTTPUtil.compile;

interface ISylu {
    AtomicReference<ISylu> INSTANCE = new AtomicReference<>();

    String login(LoginAuthorization authorization) throws IOException;

    List<ClassUnit> getClassTable(String cookie, String stuID, Term term) throws IOException;

    void logout(String cookie) throws IOException;

    Map<String,List<GPAScore>> getGPAScores(String cookie,String stuID) throws IOException;

    List<GPAScore> getInnovationByTag(String cookie, String stuID, String name) throws IOException;

    SchoolCalender getSchoolCalender(String userID, String cookie) throws IOException;

    Profile getUserProfile(String userID, String cookie) throws IOException;

    String initCookie();

    List<ExamResult> getExamList(String stuID, String cookie, Term query) throws IOException;

    List<List<String>> getExamInfo(String stuID,String cookie,ExamResult result) throws IOException;

    RSAPublicKey initRSAPublicKey(String cookie);

    static void setInstance(ISylu sylu) {
        INSTANCE.set(sylu);
    }

    static ISylu getInstance() {
        return Optional.ofNullable(INSTANCE.get()).orElseThrow(() -> new IllegalStateException("ISylu not inited!"));
    }

    default YearAndSemestersPicker getPicker(String cookie, String stuID) throws IOException {
        HashMap<String, String> years = new HashMap<>();
        HashMap<String, String> semesters = new HashMap<>();
        String defaultYears = null;
        String defaultTeamVal = null;

        Document document = HTTPUtil.newSession("/cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default&su=", stuID)
                .header("Cookie", cookie).get();

        assertLogin(document);

        for (Element e : Objects.requireNonNull(document.getElementById("xnm")).getElementsByTag("option")) {

            if (e.attr("selected").equals("selected")) {
                defaultYears = e.text();
            }
            years.put(e.text(), e.attr("value"));
        }

        for (Element e : Objects.requireNonNull(document.getElementById("xqm")).getElementsByTag("option")) {
            if (!e.attr("selected").equals("")) {
                defaultTeamVal = e.text();
            }
            semesters.put(e.text(), e.attr("value"));
        }
        Term term = new Term(defaultYears, defaultTeamVal);
        return new YearAndSemestersPicker(years, semesters, term);
    }

    default void assertLogin(String str) {
        Document d = Jsoup.parse(str);
        assertLogin(d);
    }

    default void assertLogin(Document doc) {
        for (Element e : doc.getElementsByTag("h5")) {
            if (e.text().equals("用户登录")) {

                Element test = doc.getElementById("tips");

                if (test != null) {
                    Element captcha = doc.getElementById("yzmPic");
                    if (captcha != null) {
                        String captchaLink = compile("/kaptcha?time=" + new Date().getTime());
                        throw new LoginException.NeedCaptcha(captchaLink);
                    }
                    throw new LoginException.CookieOutOfDate(test.text());
                }

                throw new LoginException.CookieOutOfDate();
            }
        }
    }
}
