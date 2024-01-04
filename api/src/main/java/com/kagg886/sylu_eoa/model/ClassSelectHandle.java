package com.kagg886.sylu_eoa.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.Map;

/**
 * @Author kagg886
 * @Date 2024/1/4 上午10:33
 * @description:
 */
@Data
public class ClassSelectHandle {
    @JSONField(name = "jsxx")
    private String teacher;

    @JSONField(name = "jxdd")
    private String room;

    @JSONField(name = "jxbrl")
    private int max;

    @JSONField(deserialize = false, serialize = false)
    private Map<String, String> context;

    @JSONField(serialize = false, deserialize = false)
    private SelectableClasses classes;

    @JSONField(serialize = false, deserialize = false)
    private String shortID;

//"bxbj": "0",
//"date": "二○二四年一月四日",
//"dateDigit": "2024年1月4日",
//"dateDigitSeparator": "2024-1-4",
//"day": "4",
//"do_jxb_id": "08688bb8d2ebe6d607dc55f0834c07ba08aabe18c3976b1efa72ee94ff87d7abae1c4faf66be9f5732e48fb3d5b35e0dd8f734caa1d308e3b722606e5e3a407d94f37a733eb3d34cf71be92f83c451f0d4e2107d9e865fecac087c7d03b89556a0ab7da4da3b85079311ba27b93ffc5de91addddcfd38c4e2ded8d51f78d3a3b",
//"fxbj": "0",
//"jgpxzd": "1",
//"jsxx": "1000035\/李诺男\/讲师（高校教师）",
//"jxb_id": "0C88E2781227E427E0630200050A4AAE",
//"jxbrl": "30",
//"jxdd": "A-302",
//"jxms": "中文教学",
//"kcgsmc": "人文素质教育模块",
//"kclbmc": "选修课",
//"kcxzmc": "通识教育理论选修",
//"kkxymc": "艺术设计学院",
//"listnav": "false",
//"localeKey": "zh_CN",
//"month": "1",
//"pageTotal": 0,
//"pageable": true,
//"queryModel": {
//"currentPage": 1,
//"currentResult": 0,
//"entityOrField": false,
//"limit": 15,
//"offset": 0,
//"pageNo": 0,
//"pageSize": 15,
//"showCount": 10,
//"sorts": [],
//"totalCount": 0,
//"totalPage": 0,
//"totalResult": 0
//},
//"rangeable": true,
//"sksj": "星期一第9-10节{1-12周}",
//"totalResult": "0",
//"userModel": {
//"monitor": false,
//"roleCount": 0,
//"roleKeys": "",
//"roleValues": "",
//"status": 0,
//"usable": false
//},
//"xqh_id": "1",
//"xqumc": "本部",
//"year": "2024",
//"yqmc": "--"
}
