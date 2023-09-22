package com.kagg886.sylu_eoa;

import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.model.ClassUnit;
import org.junit.jupiter.api.Test;

/**
 * @author kagg886
 * @date 2023/9/22 16:19
 **/
public class SyluUserTest {
    private static SyluUser user;
//    @BeforeAll
//    static void init() {
//        ISylu.setInstance(new ISyluImpl());
//        user = SyluUser.createUser("2203050528");
//        user.loginByPwd("Baleitem103");
//    }


    @Test
    void getCourse() {
        String str = "{\"dayInWeek\":1,\"lesson\":{\"end\":2,\"start\":1,\"type\":\"ALL\"},\"name\":\"概率论与数理统计A\",\"room\":\"A-319\",\"teacher\":\"张伟科\",\"weekAsMinMax\":[{\"end\":5,\"start\":1,\"type\":\"ALL\"},{\"end\":12,\"start\":7,\"type\":\"ALL\"}],\"weekEachLesson\":\"1-5周,7-12周\"}";
        ClassUnit u = JSON.parseObject(str, ClassUnit.class);
        System.out.println(u);
        System.out.println(JSON.toJSONString(u));
    }
}
