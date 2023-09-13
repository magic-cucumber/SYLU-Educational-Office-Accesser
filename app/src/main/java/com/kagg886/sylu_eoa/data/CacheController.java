package com.kagg886.sylu_eoa.data;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.model.ClassUnit;
import com.kagg886.sylu_eoa.model.ExamResult;
import com.kagg886.sylu_eoa.model.SchoolCalender;
import com.kagg886.sylu_eoa.model.YearAndSemestersPicker;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kagg886
 * @date 2023/9/9 12:08
 **/
@Data
public class CacheController {
    @JSONField(deserializeUsing = Descriptor.class)
    private List<ClassUnit> course;
    private long courseOutOfDateTimeStamp;

    private SchoolCalender calender;
    private long calenderOutOfDateTimeStamp;

    private YearAndSemestersPicker picker;
    private long pickerOutOfDateTimeStamp;

    public List<ClassUnit> getCourseBeforeOutOfDate(SyluUser user) throws RuntimeException {
        if (System.currentTimeMillis() - getCourseOutOfDateTimeStamp() > 0 && user != null) { //缓存过期，拉取最新课表
            List<ClassUnit> units = user.getClassTableByTerm(user.getSchoolCalender().getCurrentTerm());
            setCourse(units);
            setCourseOutOfDateTimeStamp(System.currentTimeMillis() + 604800000L); //7天刷新一次
            return units;
        }
        return getCourse();
    }

    public SchoolCalender getSchoolCalenderBeforeOutOfDate(SyluUser user) throws RuntimeException {
        if (System.currentTimeMillis() - getCalenderOutOfDateTimeStamp() > 0 && user != null) { //缓存过期，拉取最新课表
            SchoolCalender calender = user.getSchoolCalender();
            setCalender(calender);
            setCalenderOutOfDateTimeStamp(System.currentTimeMillis() + 604800000L); //7天刷新一次
            return calender;
        }
        return getCalender();
    }

    public YearAndSemestersPicker getPickerBeforeOutOfDate(SyluUser user) throws RuntimeException {
        if (System.currentTimeMillis() - getPickerOutOfDateTimeStamp() > 0 && user != null) { //缓存过期，拉取最新课表
            YearAndSemestersPicker exam = user.getPicker();
            setPicker(exam);
            setPickerOutOfDateTimeStamp(System.currentTimeMillis() + 604800000L); //7天刷新一次
            return exam;
        }
        return getPicker();
    }


    public static class Descriptor implements ObjectReader<List<ClassUnit>> {

        @Override
        public List<ClassUnit> readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            return ((JSONArray) jsonReader.readArray())
                    .stream()
                    .map((v) -> JSON.parseObject(v.toString(), ClassUnit.class))
                    .collect(Collectors.toList());
        }
    }

    public static class Descriptor1 implements ObjectReader<List<ExamResult>> {

        @Override
        public List<ExamResult> readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            return ((JSONArray) jsonReader.readArray())
                    .stream()
                    .map((v) -> JSON.parseObject(v.toString(), ExamResult.class))
                    .collect(Collectors.toList());
        }
    }
}
