package com.kagg886.sylu_eoa;

import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.*;
import com.kagg886.sylu_eoa.util.HTTPUtil;
import lombok.SneakyThrows;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public interface ISylu {
    AtomicReference<ISylu> INSTANCE = new AtomicReference<>();

    String login(LoginAuthorization authorization) throws IOException;

    List<ClassUnit> getClassTable(String cookie, String stuID, Term term) throws IOException;

    void logout(String cookie) throws IOException;

    Map<String,List<GPAScore>> getGPAScores(String cookie,String stuID) throws IOException;

    List<GPAScore> getInnovationByTag(String cookie, String stuID, String name) throws IOException;

    SchoolCalender getSchoolCalender(String userID, String cookie) throws IOException;

    Profile getUserProfile(String userID, String cookie) throws IOException;

    LoginAuthorization initAuthorization();

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

    default void assertLogin(Document doc) throws IOException {
        for (Element e : doc.getElementsByTag("h5")) {
            if (e.text().equals("用户登录")) {

                Element test = doc.getElementById("tips");

                if (test != null) {
                    throw new LoginException.CookieOutOfDate(test.text());
                }

                throw new LoginException.CookieOutOfDate();
            }
        }
    }

    @SneakyThrows
    default void assertLogin(String cookie) {
        assertLogin(HTTPUtil.newSession("/xtgl/index_initMenu.html")
                .header("Cookie", cookie).get());
    }

    List<SelectableClasses> getAllSelectableClass(String cookie, String stuID);

    ClassSelectHandle getSelectHandle(String cookie, String stuID, SelectableClasses selectableClasses);

    void selectClass(String cookie, String stuID, ClassSelectHandle info);

    void unselectClass(String cookie, String stuID, ClassSelectHandle handle);

    List<RelatedItem> getAllUnRelatedItem(String cookie, String stuID);


    RelatedQuestions getRelatedQuestions(String cookie, String stuID, RelatedItem item);

    void submitRelatedQuestions(String cookie, String stuID, RelatedQuestions item);
}
