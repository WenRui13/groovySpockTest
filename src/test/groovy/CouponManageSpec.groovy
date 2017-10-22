import org.apache.commons.lang3.time.DateUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification

import static com.jd.pop.qa.FileRwTool.readFromFile
import static org.apache.commons.lang3.time.DateUtils.parseDate
import static org.apache.http.impl.client.HttpClients.createDefault
import static org.apache.http.util.EntityUtils.toString
import static org.jsoup.Jsoup.parse

class CouponManageSpec extends Specification {

    @Shared
    String cookies = ""

    @Shared
    def client = createDefault()

    @Shared
    Set<String> shopNames

    def setup() {
        shopNames = readFromFile("src/test/resources/shopName.txt")
    }

    def "恢复优惠券"() {
        Set<String> coupons = collectCoupons(300)
        def pin = cookies.split(";").find { it.trim().startsWith("pin=") }.split("=")[1]
        expect:
        coupons
        coupons.every { couponId ->
            println "恢复$pin 的优惠券$couponId"
            def post = new HttpPost("https://quan.jd.com/unlock_coupon.action?pin=$pin&couponId=$couponId")
            post.setHeader("cookie", cookies)
            def response = client.execute(post)

            println toString(response.entity)
            response.statusLine.statusCode == 200
        }

    }

    Set<String> collectCoupons(int pageNum) {
        Set<String> coupons = new HashSet<String>()
        (1..pageNum).each {
            def httpGet = new HttpGet("https://quan.jd.com/delete_quan.action?page=$it")
            println "当前第$it 页……………………………………………………………………………………………………………………………………………………………………………………………………………………………………"
            httpGet.setHeader("cookie", cookies)
            def response = client.execute(httpGet)
            def entity = response.getEntity()
            def document = parse(toString(entity))

            List<String> c_msgs = new ArrayList<>()
            List<String> c_times = new ArrayList<>()

            document.getElementsByClass("coupon-item coupon-item-j ").each {
                it.getElementsByClass("c-msg").each {
                    c_msgs << it.getElementsByClass("txt").text().trim()
                }
            }

            document.getElementsByClass("coupon-item coupon-item-j ").each {
                c_times << it.getElementsByClass("c-time").text().trim()
            }


            c_msgs.eachWithIndex { String cMsg, int i ->
                String[] msgLine = cMsg.split(" ")
                def validEndDate = c_times[i].split("-")[1]
                Date date = parseDate(validEndDate, "yyyy.MM.dd")
                boolean isValid = DateTime.now().isBefore(date.getTime()) || DateUtils.isSameDay(DateTime.now().toDate(), date);
                if (belongShopSet(msgLine[0], shopNames) && msgLine[1] == "全平台" && isValid) {
                    println c_times[i] + cMsg
                    coupons.add(msgLine[2] as String)
                }
            }
        }


        return coupons
    }


    private boolean belongShopSet(String shopName, Set<String> shopSet) {
        for (String aShopSet : shopSet) {
            if (shopName.contains(aShopSet)) {
                return true
            }

        }
        return false
    }
}