package com.kagg886.sylu_eoa.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 无
 *
 * @author kagg886
 * @date 2023/9/4 19:55
 **/
@Data
@ToString
public class ClassUnit {
    public static ClassUnit EMPTY = new ClassUnit(null, null, null, null, null, "0");

    private String name;
    private String teacher;
    private String room;
    private String lesson;
    private int dayInWeek;
    private List<Range> weekAsMinMax;
    private String weekEachLesson;

    public ClassUnit(
            @JSONField(name = "name") String name,
            @JSONField(name = "teacher") String teacher,
            @JSONField(name = "room") String room,
            @JSONField(name = "weekEachLesson") String weekEachLesson,
            @JSONField(name = "lesson") String lesson,
            @JSONField(name = "dayInWeek") String dayInWeek
//            @JSONField(name = "kcmc") String name,
//            @JSONField(name = "xm") String teacher,
//            @JSONField(name = "cdmc") String room,
//            @JSONField(name = "zcd") String weekEachLesson,
//            @JSONField(name = "jcs") String lesson,
//            @JSONField(name = "xqj") String dayInWeek
    ) {
        if (name == null) {
            return;
        }
        this.name = name;
        this.teacher = teacher;
        this.room = room;
        this.lesson = lesson;
        this.dayInWeek = Integer.parseInt(dayInWeek);
        this.weekEachLesson = weekEachLesson;

        List<Range> rtn = new ArrayList<>();
        for (String a : weekEachLesson.split(",")) {
            a = a.substring(0, a.length() - 1);
            if (a.contains("-")) {
                String[] k = a.split("-");
                int l;
                FilterType type = FilterType.ALL;
                try {
                    l = Integer.parseInt(k[1]);
                } catch (NumberFormatException e) {
                    l = Integer.parseInt(k[1].split("周")[0]);
                    switch (k[1].split("\\(")[1]) {
                        case "单" -> type = FilterType.SINGULAR;
                        case "双" -> type = FilterType.EVEN;
                    }
                }
                rtn.add(new Range(Integer.parseInt(k[0]), l, type));
                continue;
            }
            rtn.add(new Range(Integer.parseInt(a), Integer.parseInt(a), FilterType.ALL));
        }
        this.weekAsMinMax = rtn;
    }


    @Data
    @ToString
    public static class Range {
        private int start;
        private int end;
        private FilterType type;

        public Range(int start, int end, FilterType type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        public Range() {

        }

        public static String formatToString(Range r) {
            StringBuilder builder = new StringBuilder();
            builder.append("第").append(r.start);
            if (r.start == r.end) {
                return builder.append("周").toString();
            }
            builder.append("周---第").append(r.end).append("周");
            if (r.getType() != FilterType.ALL) {
                builder.append("(").append(r.getType() == FilterType.SINGULAR ? "单" : "双").append("周)");
            }
            return builder.toString();
        }
    }

    public enum FilterType {
        ALL, //单双周
        SINGULAR,//单周
        EVEN;//双周
    }
}
