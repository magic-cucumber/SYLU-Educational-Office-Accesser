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
import java.util.function.BiFunction;
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

    @SneakyThrows
    @Override
    public List<SelectableClasses> getAllSelectableClass(String cookie, String stuID) {
        Connection client = HTTPUtil.newSession("/xsxk/zzxkyzb_cxZzxkYzbIndex.html?gnmkdm=N253512&layout=default&su=" + stuID);
        client.header("Cookie", cookie);

        Document doc = client.get();

        //全局上下文
        HashMap<String, String> map = new HashMap<>();

        //收集全局上下文
        doc.getElementsByTag("input").stream().filter((v) -> v.attr("type").equals("hidden")).reduce(map, new BiFunction<HashMap<String, String>, Element, HashMap<String, String>>() {
            @Override
            public HashMap<String, String> apply(HashMap<String, String> stringStringHashMap, Element element) {
                stringStringHashMap.put(element.attr("id"), element.attr("value"));
                return stringStringHashMap;
            }
        }, (stringStringHashMap, stringStringHashMap2) -> stringStringHashMap);

        //jg_id是个例外
        map.put("jg_id", doc.getElementsByTag("input").stream().filter(v -> v.attr("id").equals("jg_id_1")).findFirst().get().attr("value"));


        //收集选课上下文
        client = HTTPUtil.newSession("/xsxk/zzxkyzb_cxZzxkYzbDisplay.html?gnmkdm=N253512&su=" + stuID);
        client.header("Cookie", cookie);
        client.requestBody("xkkz_id=0DA0379132547280E0630200050A35BC&xszxzt=1&kspage=0&jspage=0"); //0DA072DE12462DBAE0630200050AC73C为体育课

        doc = client.post();
        doc.getElementsByTag("input").stream().filter((v) -> v.attr("type").equals("hidden")).reduce(map, new BiFunction<HashMap<String, String>, Element, HashMap<String, String>>() {
            @Override
            public HashMap<String, String> apply(HashMap<String, String> stringStringHashMap, Element element) {
                stringStringHashMap.put(element.attr("id"), element.attr("value").equals("null") ? "" : element.attr("value"));
                return stringStringHashMap;
            }
        }, (stringStringHashMap, stringStringHashMap2) -> stringStringHashMap);


        String[] queryModel = new String[]{
                "rwlx",
                "xkly",
                "bklx_id",
                "sfkkjyxdxnxq",
                "xqh_id",
                "njdm_id_1",
                "zyh_id_1",
                "zyh_id",
                "zyfx_id",
                "njdm_id",
                "bh_id",
                "jg_id",
                "xbm",
                "xslbdm",
                "mzm",
                "xz",
                "ccdm",
                "xsbj",
                "sfkknj",
                "sfkkzy",
                "kzybkxy",
                "sfznkx",
                "zdkxms",
                "sfkxq",
                "sfkcfx",
                "kkbk",
                "kkbkdj",
                "sfkgbcx",
                "sfrxtgkcxd",
                "tykczgxdcs",
                "xkxnm",
                "xkxqm",
                "bbhzxjxb",
                "rlkz",
                "xkzgbj",
                "jxbzb"
        };


        client = HTTPUtil.newSession("/xsxk/zzxkyzb_cxZzxkYzbPartDisplay.html?gnmkdm=N253512&su=" + stuID);
        client.header("Cookie", cookie);

        for (String s : queryModel) {
            client.data(s, map.get(s));
        }

        client.data("kklxdm", "10");
        client.data("kspage", "1");
        client.data("jspage", "10000");

        Connection.Response resp = client.method(Connection.Method.POST).execute();

        List<SelectableClasses> selectableClasses = new ArrayList<>();
        JSONArray array = JSON.parseObject(resp.body()).getJSONArray("tmpList");

        for (Object o : array) {
            JSONObject object = ((JSONObject) o);

            SelectableClasses clazz = object.to(SelectableClasses.class);
            clazz.setContext(new HashMap<>(map));
            //将api返回的上下文补全
            object.entrySet().forEach((v) -> {
                clazz.getContext().put(v.getKey(), v.getValue().toString());
            });
            selectableClasses.add(clazz);

        }

        return selectableClasses;
//        return JSON.parseObject(resp.body()).getList("tmpList", Classification.class).stream()
//                .peek((v) -> v.setContext(new HashMap<>() {{
//                    putAll(map);
//                    putAll();
//                }}))
//                .collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    public ClassSelectHandle getSelectHandle(String cookie, String stuID, SelectableClasses selectableClasses) {

//        curl 'https://jxw.sylu.edu.cn/xsxk/zzxkyzbjk_cxJxbWithKchZzxkYzb.html?gnmkdm=N253512&su=2203050528' \
//          -H 'Accept: application/json, text/javascript, */*; q=0.01' \
//          -H 'Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6' \
//          -H 'Cache-Control: no-cache' \
//          -H 'Connection: keep-alive' \
//          -H 'Content-Type: application/x-www-form-urlencoded;charset=UTF-8' \
//          -H 'Cookie: clwz_blc_pst_xd0xc2xbdxccxcexf1WEBxb7xfexcexf1xc6xf7=218105098.20480; JSESSIONID=C36A4C1A30F21E875C50546D4C22F11A' \
//          -H 'Origin: https://jxw.sylu.edu.cn' \
//          -H 'Pragma: no-cache' \
//          -H 'Referer: https://jxw.sylu.edu.cn/xsxk/zzxkyzb_cxZzxkYzbIndex.html?gnmkdm=N253512&layout=default&su=2203050528' \
//          -H 'Sec-Fetch-Dest: empty' \
//          -H 'Sec-Fetch-Mode: cors' \
//          -H 'Sec-Fetch-Site: same-origin' \
//          -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0' \
//          -H 'X-Requested-With: XMLHttpRequest' \
//          -H 'sec-ch-ua: "Not A(Brand";v="99", "Microsoft Edge";v="121", "Chromium";v="121"' \
//          -H 'sec-ch-ua-mobile: ?0' \
//          -H 'sec-ch-ua-platform: "Linux"' \
//          --data-raw 'rwlx=2&xkly=0&bklx_id=0&sfkkjyxdxnxq=0&xqh_id=1&jg_id=03&zyh_id=0305&zyfx_id=wfx&njdm_id=2022&bh_id=E06E65BE0AA38F52E0530100050A6823&xbm=1&xslbdm=wlb&mzm=01&xz=4&bbhzxjxb=0&ccdm=3&xsbj=4294967296&sfkknj=0&sfkkzy=0&kzybkxy=0&sfznkx=0&zdkxms=0&sfkxq=0&sfkcfx=0&kkbk=0&kkbkdj=0&xkxnm=2023&xkxqm=12&xkxskcgskg=1&rlkz=0&kklxdm=10&kch_id=210000038&jxbzcxskg=0&xkkz_id=0DA0379132547280E0630200050A35BC&cxbj=0&fxbj=0' \
//          --compressed


        Connection client = HTTPUtil.newSession("/xsxk/zzxkyzbjk_cxJxbWithKchZzxkYzb.html?gnmkdm=N253512&su=" + stuID);
        client.header("Cookie", cookie);

        String[] queryModel = new String[]{
                "rwlx",
                "xkly",
                "bklx_id",
                "sfkkjyxdxnxq",
                "xqh_id",
                "jg_id",
                "zyh_id",
                "zyfx_id",
                "njdm_id",
                "bh_id",
                "xbm",
                "xslbdm",
                "mzm",
                "xz",
                "bbhzxjxb",
                "ccdm",
                "xsbj",
                "sfkknj",
                "sfkkzy",
                "kzybkxy",
                "sfznkx",
                "zdkxms",
                "sfkxq",
                "sfkcfx",
                "kkbk",
                "kkbkdj",
                "xkxnm",
                "xkxqm",
                "xkxskcgskg",
                "rlkz",
                "kklxdm",
                "kch_id",
                "jxbzcxskg",
//                "xkkz_id",
                "cxbj",
                "fxbj"
        };

        for (String s : queryModel) {
            String con = selectableClasses.getContext().get(s);
            if (con == null) {
                System.out.println(s + ":null");
                continue;
            }
            client.data(s, con);
        }
        client.data("xkkz_id", "0DA0379132547280E0630200050A35BC");
        JSONArray array = JSON.parseArray(client.method(Connection.Method.POST).execute().body());

        JSONObject object = ((JSONObject) array.stream().filter((v) -> {
            JSONObject o = ((JSONObject) v);
            return o.getString("jxb_id").equals(selectableClasses.getShortID());
        }).findAny().get());

        ClassSelectHandle clazz = object.to(ClassSelectHandle.class);
        clazz.setContext(new HashMap<>(selectableClasses.getContext()));

        object.entrySet().forEach((v) -> {
            clazz.getContext().put(v.getKey(), v.getValue().toString());
        });
        return clazz;
    }

    @SneakyThrows
    @Override
    public void selectClass(String cookie, String stuID, ClassSelectHandle info) {
        Connection conn = HTTPUtil.newSession("/xsxk/zzxkyzbjk_xkBcZyZzxkYzb.html?gnmkdm=N253512&su=" + stuID);
        conn.header("Cookie", cookie);
        String[] queryModel = {
                "jxb_ids",
                "kch_id",
                "kcmc",
                "rwlx",
                "rlkz",
                "rlzlkz",
                "sxbj",
                "xxkbj",
                "qz",
                "cxbj",
//                "xkkz_id",
                "njdm_id",
                "zyh_id",
                "kklxdm",
                "xklc",
                "xkxnm",
                "xkxqm"
        };

        for (String s : queryModel) {
            String con = info.getContext().get(s);
            if (con == null) {
                System.out.println(s + ":null");
                continue;
            }
            conn.data(s, con);
        }
        //	if(rlkz=="1" || rlzlkz=="1"){
        //		sxbj = "1";
        //	}else{
        //		sxbj = "0";
        //	}
        conn.data("xkkz_id", "0DA0379132547280E0630200050A35BC");
        conn.data("jxb_ids", info.getContext().get("do_jxb_id"));
        conn.data("sxbj", (info.getContext().get("rlkz").equals("1") || info.getContext().get("rlzlkz").equals("1")) ? "1" : "0");
        conn.data("qz", "0");

        JSONObject object = JSONObject.parseObject(conn.method(Connection.Method.POST).execute().body());
        if (object.getInteger("flag") != 1) {
            throw new IllegalAccessException(object.getString("msg"));
        }
    }
}
