import com.jd.pop.qa.FileRwTool
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import spock.lang.Shared
import spock.lang.Specification

import static com.jd.pop.qa.FileRwTool.readFromFile
import static org.apache.http.impl.client.HttpClients.createDefault
import static org.apache.http.util.EntityUtils.toString
import static org.jsoup.Jsoup.parse


class CouponManageSpec extends Specification {

    @Shared
    String cookies = ""

    @Shared
    List<String> nendRecoverCouponIds = new ArrayList<>()

    @Shared
    def client = createDefault()

    @Shared
    Set<String> shopNames

    def setup() {
        shopNames = readFromFile("src/test/resources/shopName.txt")
    }

    def "恢复优惠券"() {
        Set<String> coupons = collectCoupons(700)

        expect:
        def httpGet = new HttpGet("https://quan.jd.com/delete_quan.action?page=7")
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

        c_msgs.collect {
            it.split(" ")
        }

    }

    Set<String> collectCoupons(int i) {
        def coupons = new HashSet<String>()
        (1..i).each {
            def httpGet = new HttpGet("https://quan.jd.com/delete_quan.action?page=$it")

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

        }


    }
}