package com.kagg886.sylu_eoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户的基本信息
 *
 * @author kagg886
 * @date 2023/9/4 10:42
 **/
@Data
@AllArgsConstructor
public class Profile {
    private String name;
    private String avatarLink;
    private String collegeName;
}
