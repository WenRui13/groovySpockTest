import org.apache.commons.lang3.time.DateUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification

import java.util.regex.Matcher
import java.util.regex.Pattern

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

    @Shared
    Map<String, Integer> couponPriceMap = new HashMap()


    def setup() {
        shopNames = readFromFile("src/test/resources/shopName.txt")
    }


    def "check所有可恢复coupons"() {
        Set<String> coupons = collectCoupons2delete(10000)
        String pin = getPinFromCookies(cookies)
        boolean deleteResult = coupons.isEmpty() ? true : executeDelete(coupons, pin)

        expect:
        deleteResult

        when:
        coupons = collectRecoverableCoupons(70, 2000)

        then:
        coupons
        println "there are " + coupons.size() + " coupons can be recover."
        couponPriceMap.each { entry -> println entry.key + "，优惠共计${entry.value}元" }
    }

    def "删除后恢复默认"() {
        Set<String> coupons = collectCoupons2delete(10000)
        String pin = getPinFromCookies(cookies)
        boolean deleteResult = coupons.isEmpty() ? true : executeDelete(coupons, pin)

        expect:
        deleteResult

        when:
        coupons = collectRecoverableCoupons(100, 1000)

        then:
        coupons
        executeRecover(coupons, pin)

        println "there are " + coupons.size() + " coupons have been recoved successfully."
        couponPriceMap.each { entry -> println entry.key + "，优惠共计${entry.value}元" }


    }

    def "删除后恢复"() {
        Set<String> coupons = collectCoupons2delete(10000)
        String pin = getPinFromCookies(cookies)
        boolean deleteResult = coupons.isEmpty() ? true : executeDelete(coupons, pin)

        expect:
        deleteResult

        when:
        coupons = collectCoupons2recover(2000)

        then:
        coupons
        executeRecover(coupons, pin)

        println "there are " + coupons.size() + " coupons have been recoved successfully."
        couponPriceMap.each { entry -> println entry.key + "，优惠共计${entry.value}元" }


    }

    def "删除所有coupons"() {
        Set<String> coupons = collectCoupons2delete(10000)
        String pin = getPinFromCookies(cookies)

        expect: "收集成功，并删除成功"
        coupons
        executeDelete(coupons, pin)

    }


    def "恢复优惠券"() {
        Set<String> coupons = collectCoupons2recover(1000)
        String pin = getPinFromCookies(cookies)

        expect: "收集成功，并恢复成功"
        coupons
        executeRecover(coupons, pin)

        println "there are " + coupons.size() + " coupons have been recoved successfully."
        couponPriceMap.each { entry -> println entry.key + "，优惠共计${entry.value}元" }


    }

    private static String getPinFromCookies(String cookies) {
        Pattern pattern = Pattern.compile(";\\s*pin=(.*?);")
//        Pattern pattern = Pattern.compile("\\Wpin=(.*?);")
        Matcher matcher = pattern.matcher(cookies)

        String pin = matcher.find() ? matcher.group(1) : null
        return pin
    }


    private executeRecover(Set<String> coupons, pin) {
        return coupons.every { couponId ->
            println "恢复$pin 的优惠券$couponId"
            def post = new HttpPost("https://quan.jd.com/unlock_coupon.action?pin=$pin&couponId=$couponId")
            post.setHeader("cookie", cookies)
            def response = client.execute(post)

            println toString(response.entity)
            response.statusLine.statusCode == 200
        }
    }

    private executeDelete(Set<String> coupons, pin) {
        return coupons.every { couponId ->
            println "删除$pin 的优惠券$couponId"
            def post = new HttpPost("https://quan.jd.com/lock_coupon.action?pin=$pin&couponId=$couponId")
            post.setHeader("cookie", cookies)
            def response = client.execute(post)

            println toString(response.entity)
            response.statusLine.statusCode == 200

        }
    }

    Set<String> collectCoupons2delete(int pageNum) {
        Set<String> coupons = new HashSet<String>()
        for (i in (1..pageNum)) {
            def httpGet = new HttpGet("https://quan.jd.com/user_quan.action?couponType=-1&sort=3&page=$i")
            println "当前第$i 页……………………………………………………………………………………………………………………………………………………………………………………………………………………………………"
            httpGet.setHeader("cookie", cookies)
            def response = client.execute(httpGet)
            def entity = response.getEntity()
            def document = parse(toString(entity))

            if (document.title().contains("京东-欢迎登录")) {
                println document.title()
                throw new RuntimeException("cookie无效，请重新登录！！！")
            }


            if (document.getElementsByClass("coupon-item").isEmpty()) {
                println "当前页面无优惠券**************"
                return coupons

            }

            document.getElementsByClass("coupon-item").each {
                coupons << it.getElementsByClass("txt").text().trim().split("\\s+")[2]
            }
        }


        return coupons
    }

    Set<String> collectCoupons2recover(int pageNum) {
        Set<String> coupons = new HashSet<String>()
        for (i in (1..pageNum)) {
            def httpGet = new HttpGet("https://quan.jd.com/delete_quan.action?page=$i")
            println "当前第$i 页……………………………………………………………………………………………………………………………………………………………………………………………………………………………………"
            httpGet.setHeader("cookie", cookies)
            def response = client.execute(httpGet)
            def entity = response.getEntity()
            def document = parse(toString(entity))

            if (document.title().contains("京东-欢迎登录")) {
                println document.title()
                throw new RuntimeException("cookie无效，请重新登录！！！")
            }


            if (document.getElementsByClass("coupon-item").isEmpty()) {
                println "当前页面无优惠券**************"
                couponPriceMap = couponPriceMap.sort { a, b -> b.value <=> a.value }
                return coupons

            }


            List<String> c_msgs = new ArrayList<>()
            List<String> c_times = new ArrayList<>()
            List<String> c_prices = new ArrayList<>()

            document.getElementsByClass("coupon-item").each {
                if (it.attributes().get("class").contains("coupon-item-j")
                        || it.attributes().get("class").contains("coupon-item-myf")) {

                    c_msgs << it.getElementsByClass("txt").text().trim()
                    c_times << it.getElementsByClass("c-time").text().trim()
                    c_prices << it.getElementsByClass("c-price").text().trim()
                }

            }

            c_msgs.eachWithIndex { String cMsg, int j ->
                String[] msgLine = cMsg.split("\\s+")
//                String[] priceInfo = c_prices[j].split("\\s+")

                String couponPrice = c_prices[j].replaceAll("\\D", "")
                String validEndDate = c_times[j].replaceAll(".*-", "")
                Date date = parseDate(validEndDate, "yyyy.MM.dd")
                boolean isValid = DateTime.now().isBefore(date.getTime()) || DateUtils.isSameDay(DateTime.now().toDate(), date)
                if (msgLine[1] == "全平台" && isValid && (belongShopSet(msgLine[0], shopNames) || msgLine[0].contains("部分"))) {
                    println c_times[j] + cMsg
                    coupons.add(msgLine[2] as String)
                    def totalPrice = couponPriceMap.containsKey(msgLine[0]) ? couponPriceMap.get(msgLine[0]) + Integer.valueOf(couponPrice) : Integer.valueOf(couponPrice)
                    couponPriceMap.put(msgLine[0], totalPrice)
                    if (coupons.size() > 200) {
                        throw new Exception("the number of coupons to be recoved will be greater than 200")
                    }
                }
            }
        }

        couponPriceMap = couponPriceMap.sort { a, b -> b.value <=> a.value }


        return coupons
    }

    Set<String> collectRecoverableCoupons(int threshold, int pageNum) {
        Set<String> coupons = new HashSet<String>()
        Map<String, List<String>> shopCouponsMap = [:]
        for (i in (1..pageNum)) {
            def httpGet = new HttpGet("https://quan.jd.com/delete_quan.action?page=$i")
            println "当前第$i 页……………………………………………………………………………………………………………………………………………………………………………………………………………………………………"
            httpGet.setHeader("cookie", cookies)
            def response = client.execute(httpGet)
            def entity = response.getEntity()
            def document = parse(toString(entity))

            if (document.title().contains("京东-欢迎登录")) {
                println document.title()
                throw new RuntimeException("cookie无效，请重新登录！！！")
            }


            if (document.getElementsByClass("coupon-item").isEmpty()) {
                println "当前页面无优惠券**************"
                filterCoupons(threshold, shopCouponsMap, coupons)
                return coupons

            }


            List<String> c_msgs = new ArrayList<>()
            List<String> c_times = new ArrayList<>()
            List<String> c_prices = new ArrayList<>()

            document.getElementsByClass("coupon-item").each {
                if (it.attributes().get("class").contains("coupon-item-j")
                        || it.attributes().get("class").contains("coupon-item-myf")) {

                    c_msgs << it.getElementsByClass("txt").text().trim()
                    c_times << it.getElementsByClass("c-time").text().trim()
                    c_prices << it.getElementsByClass("c-price").text().trim()
                }

            }

            c_msgs.eachWithIndex { String cMsg, int j ->
                String[] msgLine = cMsg.split("\\s+")
//                String[] priceInfo = c_prices[j].split("\\s+")

                String couponPrice = c_prices[j].replaceAll("\\D", "")
                String validEndDate = c_times[j].replaceAll(".*-", "")
                Date date = parseDate(validEndDate, "yyyy.MM.dd")
                boolean isValid = DateTime.now().isBefore(date.getTime()) || DateUtils.isSameDay(DateTime.now().toDate(), date)
                if (msgLine[1] == "全平台" && isValid) {
                    println c_times[j] + cMsg
//                    coupons.add(msgLine[2] as String)

                    def shopCoupons = shopCouponsMap.containsKey(msgLine[0]) ? shopCouponsMap.get(msgLine[0]) << msgLine[2] : [msgLine[2]]
                    shopCouponsMap.put(msgLine[0], shopCoupons)

                    def totalPrice = couponPriceMap.containsKey(msgLine[0]) ? couponPriceMap.get(msgLine[0]) + Integer.valueOf(couponPrice) : Integer.valueOf(couponPrice)
                    couponPriceMap.put(msgLine[0], totalPrice)
                }
            }
        }

        filterCoupons(threshold, shopCouponsMap, coupons)

        return coupons
    }

    private filterCoupons(int threshold, LinkedHashMap<String, List<String>> shopCouponsMap, coupons) {
        couponPriceMap = couponPriceMap.findAll { entry -> entry.value >= threshold }.sort { a, b -> b.value <=> a.value }
        shopCouponsMap.each { entry ->
            if (couponPriceMap.containsKey(entry.key)) {
                coupons.addAll(entry.value)
            }
        }
    }


    private static boolean belongShopSet(String shopName, Set<String> shopSet) {
        for (String aShopSet : shopSet) {
            if (shopName.contains(aShopSet)) {
                return true
            }

        }
        return false
    }
}