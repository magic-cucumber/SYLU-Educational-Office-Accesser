package com.kagg886.sylu_eoa.data;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.kagg886.sylu_eoa.model.ClassUnit;
import com.kagg886.sylu_eoa.model.SchoolCalender;
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


    public static class Descriptor implements ObjectReader<List<ClassUnit>> {

        @Override
        public List<ClassUnit> readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            return ((JSONArray) jsonReader.readArray())
                    .stream()
                    .map((v) -> JSON.parseObject(v.toString(), ClassUnit.class))
                    .collect(Collectors.toList());
        }
    }
}
