package com.kagg886.sylu_eoa.data;

import lombok.Data;
import lombok.ToString;

/**
 * @author kagg886
 * @date 2023/9/6 18:29
 **/
@Data
@ToString
public class LoginModel {
    private String userID;
    private String cookie;
}
