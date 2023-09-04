package com.kagg886.sylu_eoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学期起止时间和周数
 *
 * @author kagg886
 * @date 2023/9/4 11:20
 **/
@Data
@AllArgsConstructor
public class SchoolCalender {
    private LocalDate start;
    private LocalDate end;
    private Term currentTerm;
}
