package com.kagg886.sylu_eoa.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * @Author kagg886
 * @Date 2024/1/11 下午2:48
 * @description: 未完成的学生评价
 */
@Data
public class RelatedItem {
    @JSONField(name = "jzgmc")
    private String teacher;

    @JSONField(name = "jxdd")
    private String room;

    @JSONField(name = "kcmc")
    private String name;

    private String jxb_id;
    private String jgh_id;
    private String kch_id;
    private String xsdm;

    //提交状态
    private int tjzt;

    public boolean isSubmit() {
        return tjzt == 1;
    }
    //date: "二○二四年一月十一日"
    //dateDigit: "2024年1月11日"
    //dateDigitSeparator: "2024-1-11"
    //day: "11"
    //jgh_id: "86c52432bf98510b5a317522fc4e945d0938a899683db2b65c58a178816f11d9de3b11db17cb64e2582350c0ca30fddfcb22da657c9fccf53b743f2abeef8f4d7e76c65ec46835a2029e1c4fae6d18cf0ca38090753cf1fa2e4de2b5daf133fe23104160dabae4ef7e1d33e8bad0a5bfd7e6172056280ec206e0c95132cd3f2e"
    //jgpxzd: "1"
    //jxb_id: "FFB4237F821E29B1E0530100050AA647"
    //jxbmc: "(2023-2024-1)-210000018-01"
    //jxdd: "A-212"
    //jzgmc: "洪菊华"
    //kch_id: "210000018"
    //kcmc: "园中画境-古典园林赏析"
    //listnav: "false"
    //localeKey: "zh_CN"
    //month: "1"
    //pageTotal: 0
    //pageable: true
    //pjzt: "0"
    //queryModel: {currentPage: 1, currentResult: 0, entityOrField: false, limit: 15, offset: 0, pageNo: 0, pageSize: 15,…}
    //rangeable: true
    //row_id: "1"
    //sfcjlrjs: "1"
    //sksj: "星期日第1-2节{7-18周}"
    //tjzt: "-1"
    //tjztmc: "未评"
    //totalResult: "27"
    //userModel: {monitor: false, roleCount: 0, roleKeys: "", roleValues: "", status: 0, usable: false}
    //xnm: "2023"
    //xqm: "3"
    //xsdm: "01"
    //xsmc: "讲课学时"
    //year: "2024"
}
