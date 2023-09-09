package com.kagg886.sylu_eoa.data;

import com.kagg886.sylu_eoa.model.ClassUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课表查询工具类
 *
 * @author kagg886
 * @date 2023/9/9 11:53
 **/

@EqualsAndHashCode(callSuper = true)
@Data
public class ClassTable extends ArrayList<ClassUnit> {

    public ClassTable() {

    }

    public ClassTable(List<ClassUnit> table) {
        addAll(table);
    }

    public ClassTable queryClassByWeek(int week) {
        ClassTable rtn = new ClassTable();
        rtn.addAll(this.stream().filter(classUnit -> {
            for (ClassUnit.Range range : classUnit.getWeekAsMinMax()) {
                if (week >= range.getStart() && week <= range.getEnd()) {
                    switch (range.getType()) {
                        case ALL:
                            return true;
                        case SINGULAR:
                            return week % 2 == 1;
                        case EVEN:
                            return week % 2 == 0;
                    }
                }
            }
            return false;
        }).collect(Collectors.toList()));
        return rtn;
    }

    public ClassTable queryClassByLesson(String lesson) {
        ClassTable rtn = new ClassTable();
        rtn.addAll(this.stream().filter(classUnit -> classUnit.getLesson().equals(lesson)).collect(Collectors.toList()));
        return rtn;
    }

    public ClassTable queryClassByDay(int day) {
        ClassTable rtn = new ClassTable();
        rtn.addAll(this.stream().filter(classUnit -> classUnit.getDayInWeek() == day).collect(Collectors.toList()));
        return rtn;
    }
}
