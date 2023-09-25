package com.kagg886.sylu_eoa;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

/**
 * @author kagg886
 * @date 2023/9/25 15:14
 **/
public class OtherTest {
    @Test
    void testClass2() throws Exception {
        Connection.Response response = Jsoup.connect("http://xg.sylu.edu.cn/Sylutw/sys/SystemForm/StuAction/StuActionSearch.aspx")
                .ignoreContentType(true)
                .header("Cookie", "ASP.NET_SessionId=jmyttalv40hn2kj0jpbw1v1n; CenterSoft=B386D38ECF31648A3AA1E1516A55069DF6AE072DDCF378B8B2A3F57A9D45A4C45EE0D4692C178C266D8A6DACE113EDFFAB952A9821FE03C608547A0388C385B2816783CAD1BCD7F703224022AADBB15748439790B14DE69F77A5C6F6C830625F8DEFB3BC1F901B235ED6839899E51D6F7893C15719C45990DA46B3DFC1F115388074E3AF6EBA7FDBFA633C8099BC2EC6B0753B5ABA15F62680B14A0526564067B93F05F7FCBF297A778B5EB2FBEEC674BE42D19681457BADB397C29312774935B7F4B7253081D9C45871B192602DE21B325132808703B77D6A4245D8545F86D2E317C1A99F4C7C03588D442119A30B88")
                .execute();
//        System.out.println(response.body());

        Document doc = response.parse();

        Elements data = doc.getElementsByTag("tr");

        Element summary = data.get(0);
        System.out.println(summary);

        for (int i = 2; i < data.size(); i++) {
            Element info = data.get(i);

            Elements elements = info.getElementsByTag("td");

            System.out.println(elements.get(0).text()); //名字
            System.out.println(elements.get(1).text()); //申请单位
            System.out.println(elements.get(2).text()); //时间
            System.out.println(elements.get(3).text()); //type
            System.out.println(elements.get(4).text()); //身份
            System.out.println(elements.get(5).text()); //参与人数
            System.out.println(elements.get(7).text()); //分
            break;
        }
    }
}
