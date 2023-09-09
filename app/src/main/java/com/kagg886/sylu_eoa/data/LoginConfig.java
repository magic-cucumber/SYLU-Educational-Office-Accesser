package com.kagg886.sylu_eoa.data;

import com.kagg886.sylu_eoa.SyluUser;
import lombok.Data;

/**
 * @author kagg886
 * @date 2023/9/8 18:18
 **/
@Data
public class LoginConfig {
    @DeepListener
    private SyluUser user;
}
