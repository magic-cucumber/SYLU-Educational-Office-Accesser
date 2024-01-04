package com.kagg886.sylu_eoa.data;

import com.alibaba.fastjson2.annotation.JSONField;
import com.kagg886.sylu_eoa.SyluUser;
import lombok.Data;

/**
 * @author kagg886
 * @date 2023/9/8 18:18
 **/
@Data
public class LoginConfig {
    @JSONField(serialize = false, deserialize = false)
    private SyluUser user;

    private String id;
    private String pass;
}
