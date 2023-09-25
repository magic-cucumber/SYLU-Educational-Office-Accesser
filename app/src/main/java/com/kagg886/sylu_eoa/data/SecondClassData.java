package com.kagg886.sylu_eoa.data;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kagg886
 * @date 2023/9/17 20:49
 **/
@Data
public class SecondClassData {
    private String cookie;
    private double A, B, C, D, E, Sum;
    private double A1, B1, C1, D1, E1, Sum1;

    @JSONField(deserializeUsing = Descriptor.class)
    private List<Detail> details;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Detail {
        private String name;
        private String sponsor;
        private String time;
        private int type;
        private String actor;
        private int people;
        private double score;
    }

    private static class Descriptor implements ObjectReader<List<Detail>> {

        @Override
        public List<Detail> readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            return ((JSONArray) jsonReader.readArray())
                    .stream()
                    .map((v) -> JSON.parseObject(v.toString(), Detail.class))
                    .collect(Collectors.toList());
        }
    }
}
