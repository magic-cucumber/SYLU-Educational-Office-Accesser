package com.kagg886.sylu_eoa;

import com.kagg886.sylu_eoa.data.ClassTable;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void testClassTableSelect() {
        ISylu.setInstance(new ISyluImpl());
        SyluUser user = SyluUser.createUser("2203050528");
        user.loginByPwd("Baleitem103");

        ClassTable table = new ClassTable(user.getClassTableByTerm(user.getSchoolCalender().getCurrentTerm()));

        ClassTable table1 = table.queryClassByLesson(9, 10);

        System.out.println(table1);
    }
}