package com.kagg886.sylu_eoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Objects;

/**
 * 获得学年和学期列表
 *
 * @author kagg886
 * @date 2023/9/4 18:38
 **/
@Data
@AllArgsConstructor
public class YearAndSemestersPicker {
    private HashMap<String,String> year;
    private HashMap<String,String> semester;
    private Term defaultTerm;
}
