package com.kagg886.sylu_eoa;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.*;
import com.kagg886.sylu_eoa.util.HTTPUtil;
import com.kagg886.sylu_eoa.util.RSA;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sylu的API层集合
 *
 * @author kagg886
 * @date 2023/9/3 17:03
 **/
public class ISyluImpl implements ISylu {

    @Override
    public String login(LoginAuthorization auth) throws IOException {
        Objects.requireNonNull(auth.getUser());
        Objects.requireNonNull(auth.getCookie());
        Objects.requireNonNull(auth.getPublicKey());
        Objects.requireNonNull(auth.getPassWord());

        Connection conn = HTTPUtil.newSession("/xtgl/login_slogin.html?time=", new Date().getTime());
        conn.header("Cookie", auth.getCookie());
        conn.header("csrftoken", auth.getCsrf());
        conn.data("yhm", auth.getUser())
                .data("mm", RSA.getInstance().encrypt(auth.getPublicKey(), auth.getPassWord()));
        if (auth.getCaptcha() != null) {
            conn.data("yzm", auth.getCaptcha());
        }

        Document doc = conn.post();

        Element test = doc.getElementById("tips");
        if (test != null) {
            Element captcha = doc.getElementById("yzmPic");
            if (captcha != null) {
                byte[] image = HTTPUtil.newSession("/kaptcha?time=" + new Date().getTime())
                        .header("Cookie", auth.getCookie()).execute().bodyAsBytes();
                throw new LoginException.NeedCaptcha(image);
            }
            throw new LoginException(test.text());
        }

        return "JSESSIONID=" + conn.response().cookie("JSESSIONID");
    }

    @Override
    public List<ClassUnit> getClassTable(String cookie, String stuID, Term term) throws IOException {
        YearAndSemestersPicker picker = ISylu.getInstance().getPicker(cookie, stuID);

        String xnm = picker.getYear().get(term.getYearsOfSchooling());
        String xqm = picker.getSemester().get(term.getSemesterNumber());

        Connection.Response resp = HTTPUtil.newSession("/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=ssss&su=", stuID)
                .header("Cookie", cookie)
                .data("xnm", xnm).data("xqm", xqm)
                .data("kzlx", "ck").data("xsdm", "")
                .method(Connection.Method.POST).execute();

        String body = resp.body();
        assertLogin(Jsoup.parse(body));


        JSONArray array = JSON.parseObject(body).getJSONArray("kbList");
        if (array.isEmpty()) {
            throw new IllegalStateException("该学年学期的课表尚未开放!");
        }
        return array.stream()
                .map((v) -> {
                    JSONObject a = (JSONObject) v;
                    String lesson = a.getString("jcs");
                    String[] ls = lesson.split("-");

                    return new ClassUnit(
                            a.getString("kcmc"),
                            a.getString("xm"),
                            a.getString("cdmc"),
                            a.getString("zcd"),
                            new ClassUnit.Range(Integer.parseInt(ls[0]), Integer.parseInt(ls[1]), ClassUnit.FilterType.ALL),
                            a.getString("xqj")
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public void logout(String cookie) throws IOException {
        HTTPUtil.newSession("/logout?t=1688629994486&login_type=")
                .cookie("Cookie", cookie).method(Connection.Method.POST)
                .execute();
    }

    @Override
    public Map<String, List<GPAScore>> getGPAScores(String cookie, String stuID) throws IOException {
        HashMap<String, List<GPAScore>> map = new HashMap<>();
        Connection.Response resp = HTTPUtil.newSession("/xmfzgl/xshdfzcx_cxXshdfzcxIndex.html?doType=query&gnmkdm=N4780&su=", stuID)
                .header("Cookie", cookie)
                .data("nd", String.valueOf(System.currentTimeMillis()))
                .data("_search", "false")
                .data("queryModel.showCount", "5000")
                .data("queryModel.currentPage", "1")
                .data("queryModel.sortName:", "")
                .data("queryModel.sortOrder", "asc")
                .data("time", "0")
                .execute();
        String body = resp.body();
        assertLogin(Jsoup.parse(body));
        JSON.parseObject(body).getJSONArray("items").forEach((i) -> {
            JSONObject object = (JSONObject) i;
            String name = object.getString("xmlbmc");
            try {
                map.put(name, getInnovationByTag(cookie, stuID, name));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return map;
    }

    @Override
    public List<GPAScore> getInnovationByTag(String cookie, String stuID, String name) throws IOException {
        Connection.Response resp = HTTPUtil.newSession("/xmfzgl/xshdfzcx_cxXmfzqr.html?gnmkdm=N4780&su=", stuID)
                .header("Cookie", cookie)
                .data("xmlbmc", name)
                .method(Connection.Method.POST).execute();

        String body = resp.body();
        assertLogin(Jsoup.parse(body));

        return JSON.parseArray(JSON.parseObject(body).getJSONArray("items").toString(), new TypeReference<GPAScore>() {
        }.getType());
//        return JSON.parseObject(body).getJSONArray("items")
//                .stream()
//                .map((v) -> JSON.parseObject(v.toString(), GPAScore.class))
//                .collect(Collectors.toList());
    }

    @Override
    public SchoolCalender getSchoolCalender(String userID, String cookie) throws IOException {
        Objects.requireNonNull(userID);
        Objects.requireNonNull(cookie);

        Connection.Response resp = HTTPUtil.newSession("/xtgl/index_cxAreaSix.html?localeKey=zh_CN&gnmkdm=index&su=", userID)
                .header("Cookie", cookie)
                .method(Connection.Method.POST)
                .execute();
        Document document = Jsoup.parse(resp.body());

        assertLogin(document);
        String source = document.getElementsByAttributeValue("colspan", "24").get(0).text();

        String year = source.split("学年")[0];
        String sem = source.split("学年")[1].split("学期")[0];


        int l, r;
        l = source.indexOf("(");
        r = source.indexOf(")");
        source = source.substring(l + 1, r);
        String[] se = source.split("至");

        String[] starts = se[0].split("-");
        LocalDate start = LocalDate.of(Integer.parseInt(starts[0]), Integer.parseInt(starts[1]), Integer.parseInt(starts[2]));
        starts = se[1].split("-");
        LocalDate end = LocalDate.of(Integer.parseInt(starts[0]), Integer.parseInt(starts[1]), Integer.parseInt(starts[2]));

        Term term1 = new Term(year, sem);

        return new SchoolCalender(start, end, term1);
    }

    @Override
    public Profile getUserProfile(String userID, String cookie) throws IOException {
        Objects.requireNonNull(userID);
        Objects.requireNonNull(cookie);

        Connection client = HTTPUtil.newSession("/xsxxxggl/xsgrxxwh_cxXsgrxx.html?gnmkdm=N100801&layout=default&su=", userID);
        client.header("Cookie", cookie);
//        client.header("Referer", "https://jxw.sylu.edu.cn/xtgl/login_slogin.html?kickout=1");
        client.method(Connection.Method.GET);

        Document document = client.get();
        assertLogin(document);

        String avt = document.getElementsByTag("img").get(0).attr("src");
        byte[] a = HTTPUtil.newSession(avt).header("Cookie", cookie).execute().bodyAsBytes();

        Elements ele = document.getElementsByClass("form-control-static");

        //public Profile(     String name,
        //    String collegeName,
        //    String studyName,
        //    byte[] avatar,
        //    String email,
        //    String phone,
        //    String id,
        //    String policy,
        //    String language )
        return new Profile(
                ele.get(1).text(),
                ele.get(14).text(),
                ele.get(15).text(),
                a,
                ele.get(26).text(),
                ele.get(27).text(),
                ele.get(7).text(),
                ele.get(10).text(),
                ele.get(24).text()
        );
    }

    @SneakyThrows
    @Override
    public LoginAuthorization initAuthorization() {
        LoginAuthorization auth = new LoginAuthorization();
        try {
            Connection.Response resp = HTTPUtil.newSession("/xtgl/login_slogin.html").execute();

            String cookie = resp.header("Set-Cookie");
            if (cookie == null) {
                throw new LoginException.CookieInitFailed();
            }
            auth.setCookie(cookie.split(";")[0]);
            auth.setCsrf(Jsoup.parse(resp.body()).getElementById("csrftoken").val());
        } catch (IOException e) {
            throw new LoginException.CookieInitFailed();
        }
        return auth;
    }

    @Override
    public List<ExamResult> getExamList(String stuID, String cookie, Term query) throws IOException {
        YearAndSemestersPicker picker = getPicker(cookie, stuID);


        String xnm = picker.getYear().get(query.getYearsOfSchooling());
        String xqm = picker.getSemester().get(query.getSemesterNumber());

        try {
            Objects.requireNonNull(xnm);
            Objects.requireNonNull(xqm);
        } catch (NullPointerException e) {
            throw new IllegalStateException("学期值非法!");
        }
        Connection conn = HTTPUtil.newSession("/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005&su=", stuID)
                .header("Cookie", cookie)
                .data("xnm", xnm)
                .data("xqm", xqm)
                .data("_search", "false")
                .data("nd", String.valueOf(new Date().getTime()))
                .data("queryModel.showCount", " 50")
                .data("queryModel.currentPage", " 1")
                .data("queryModel.sortName", "")
                .data("queryModel.sortOrder", "asc")
                .data("time", "2");

        Connection.Response resp = conn.method(Connection.Method.POST).execute();
        String body = resp.body();

        assertLogin(Jsoup.parse(body));

        return JSON.parseArray(JSON.parseObject(body).getJSONArray("items").toString(), new TypeReference<ExamResult>() {
        }.getType());
//        return array.stream()
//                .map(k -> (JSONObject) k)
//                .map(v -> JSON.parseObject(v.toString(), ExamResult.class))
//                .collect(Collectors.toList());
    }

    @Override
    public List<List<String>> getExamInfo(String stuID, String cookie, ExamResult i) throws IOException {
        String xnm = i.getYear();
        String xqm = i.getSemester();

        Connection.Response resp = HTTPUtil.newSession("/cjcx/cjcx_cxCjxqGjh.html?time=", new Date().getTime(), "&gnmkdm=N305005&su=", stuID)
                .header("Cookie", cookie)
                .data("jxb_id", i.getDetailsID())
                .data("xnm", xnm)
                .data("xqm", xqm)
                .data("kcmc", i.getName())
                .method(Connection.Method.POST).execute();


        List<List<String>> rtn = new ArrayList<>();
        String body = resp.body();
        assertLogin(Jsoup.parse(body));
        Elements tr = Jsoup.parse(body).getElementsByTag("tr");
        for (int j = 1; j < tr.size(); j++) {
            List<String> trs = new ArrayList<>();
            for (Element td : tr.get(j).getElementsByTag("td")) {
                trs.add(td.text());
            }
            rtn.add(trs);
        }
        return rtn;
    }

    @Override
    public RSAPublicKey initRSAPublicKey(String cookie) {
        Objects.requireNonNull(cookie);

        Connection client = HTTPUtil.newSession("/xtgl/login_getPublicKey.html?time=", new Date().getTime(), "&_=", new Date().getTime());
        client.header("Cookie", cookie);

        Connection.Response resp;
        try {
            resp = client.execute();
        } catch (IOException e) {
            throw new IllegalStateException("无法获得通信密钥，这可能是教务网的问题\n请检查教务网网页端能够正常进入。");
        }

        return JSON.parseObject(resp.body(), RSAPublicKey.class);
    }
}
