package com.kagg886.sylu_eoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @Author kagg886
 * @Date 2024/1/11 下午2:59
 * @description: 每个老师的答题选项
 */
@Data
public class RelatedQuestion {
    private String desc;
    private List<Choice> choices;
    private int index = -1;


    private String zsmbmcb_id;
    private String pfdjdmb_id;
    private String pjzbxm_id;


    public RelatedQuestion(String desc, List<Choice> choices, String zsmbmcb_id, String pfdjdmb_id, String pjzbxm_id) {
        this.desc = desc;
        this.choices = choices;
        this.zsmbmcb_id = zsmbmcb_id;
        this.pfdjdmb_id = pfdjdmb_id;
        this.pjzbxm_id = pjzbxm_id;
    }

    public Choice getChoice() {
        return choices.get(index);
    }

    public void select(Choice a) {
        this.index = choices.indexOf(a);
    }

    @Data
    @AllArgsConstructor
    public static class Choice {
        private String pfdjdmxmb;
        private String name;


        private String value;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Choice)) {
                return false;
            }
            return ((Choice) o).value.equals(this.value);
        }
    }
}
