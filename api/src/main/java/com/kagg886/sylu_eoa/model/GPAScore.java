package com.kagg886.sylu_eoa.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * GPA绩点
 *
 * @author kagg886
 * @date 2023/9/4 20:32
 **/
@Data
public class GPAScore {
    //object.getString("xmnr"), object.getString("yxfz")

    @JSONField(name = "xmnr")
    private String name;
    @JSONField(name = "yxfz")
    private String score;
}
