package com.kagg886.sylu_eoa.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;

/**
 * @Author kagg886
 * @Date 2024/1/11 下午3:09
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RelatedQuestions extends ArrayList<RelatedQuestion> {
    private RelatedItem source;

    private String xspfb_id;
    private String pjzbxm_id;

    private String pyID;
}
