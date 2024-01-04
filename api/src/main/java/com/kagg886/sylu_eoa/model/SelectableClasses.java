package com.kagg886.sylu_eoa.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.Map;

/**
 * @Author kagg886
 * @Date 2024/1/4 上午9:30
 * @description: 选课列表
 */
@Data
public class SelectableClasses {
    //名字
    @JSONField(name = "kcmc")
    private String name;

    //模块
    @JSONField(name = "kzmc")
    private String modules;

    //学分
    @JSONField(name = "xf")
    private String score;

    @JSONField(serialize = false, deserialize = false)
    private Map<String, String> context;

    @JSONField(name = "yxzrs")
    private int count;

    @JSONField(name = "jxb_id")
    private String shortID;
//"blyxrs": "0",
//"blzyl": "0",
//"cxbj": "0",
//"date": "二○二四年一月四日",
//"dateDigit": "2024年1月4日",
//"dateDigitSeparator": "2024-1-4",
//"day": "4",
//"fxbj": "0",
//"jgpxzd": "1",
//"jxb_id": "0C83075F4236668CE0630100050A2172",
//"jxbmc": "(2023-2024-2)-210000038-01",
//"jxbzls": "1",
//"kch": "210000038",
//"kch_id": "210000038",
//"kcmc": "股票投资基础",
//"kcrow": "1",
//"kklxdm": "10",
//"kzmc": "通识选修课课,经济管理模块",
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
//"totalResult": "0",
//"userModel": {
//"monitor": false,
//"roleCount": 0,
//"roleKeys": "",
//"roleValues": "",
//"status": 0,
//"usable": false
//},
//"xf": "1.5",
//"xxkbj": "0",
//"year": "2024",
//"yxzrs": "50"
}
