package com.kagg886.sylu_eoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 学期信息
 *
 * @author kagg886
 * @date 2023/9/4 11:26
 **/
@Data
@AllArgsConstructor
public class Term {
    private String yearsOfSchooling;
    private String semesterNumber;
}
