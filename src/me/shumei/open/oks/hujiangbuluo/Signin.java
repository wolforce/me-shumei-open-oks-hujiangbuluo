package me.shumei.open.oks.hujiangbuluo;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
    String resultFlag = "false";
    String resultStr = "未知错误！";
    
    /**
     * <p><b>程序的签到入口</b></p>
     * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
     * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
     * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
     * @param cfg “配置”栏内输入的数据
     * @param user 用户名
     * @param pwd 解密后的明文密码
     * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
     */
    public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
        //把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
        CaptchaUtil.context = ctx;
        //标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
        CaptchaUtil.isAutoSign = isAutoSign;
        
        try{
            //存放Cookies的HashMap
            HashMap<String, String> cookies = new HashMap<String, String>();
            //Jsoup的Response
            Response res;
            
            //获取ssotoken的链接
            String ssotokenUrl;
            //登录链接
            String loginUrl;
            //签到链接
            String signUrl = "http://bulo.hujiang.com/app/api/ajax_take_card.ashx?" + Math.random();
            
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            String dataString = formatter.format(date);//当前日期的字符串
            
            //构造登录链接
            StringBuilder sb = new StringBuilder();
            sb.append("http://pass.hujiang.com/quick/account/");
            sb.append("?account=");
            sb.append(URLEncoder.encode(user, "UTF-8"));
            sb.append("&password=");
            sb.append(MD5.md5(pwd));
            sb.append("&code=&act=loginverify&template=undefined&returnurl=undefined&source=yeshj&rand=" + Math.random());
            ssotokenUrl = sb.toString();
            System.out.println(ssotokenUrl);
            
            //获取ssotoken
            //{"code":0,"message":"OK","data":{"userid":11284243,"username":"wolforce","ssotoken":"a5f321123154c8d75571f123456781"}}
            res = Jsoup.connect(ssotokenUrl).userAgent(UA_CHROME).cookies(cookies).referrer(ssotokenUrl).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
            cookies.putAll(res.cookies());
            System.out.println(res.body());
            
            JSONObject jsonObj = new JSONObject(res.body());
            int code = jsonObj.optInt("code");
            if(code != 0) {
                return new String[]{"false", "获取登录信息失败（无法获取ssotoken）"};
            }
            
            //使用ssotoken登录账号
            //{"code":0,"message":"OK","data":".hujiang.com"}
            String ssotoken = jsonObj.optJSONObject("data").optString("ssotoken");
            loginUrl = "http://pass.hujiang.com/quick/synclogin.aspx?token=" + ssotoken + "&remeberdays=14&rand=" + Math.random();
            res = Jsoup.connect(loginUrl).userAgent(UA_CHROME).cookies(cookies).referrer(ssotokenUrl).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
            cookies.putAll(res.cookies());
            System.out.println("=========");
            System.out.println(res.cookies());
            System.out.println(loginUrl);
            System.out.println(res.body());
            
            if(res.body().contains("\"code\":0") || res.body().contains("var _sso_hujiang = 1;"))
            {
                //登录成功
                //提交签到请求
                res = Jsoup.connect(signUrl).data("X-AjaxPro-Method", "PageCardAwardNew").cookies(cookies).userAgent(UA_CHROME).referrer(ssotokenUrl).timeout(TIME_OUT).ignoreContentType(true).method(Method.POST).execute();
                System.out.println(res.body());
                
                //用正则分析返回的数据
                //["21345","375"]
                //["","374"]
                Pattern pattern = Pattern.compile("\"(\\d*)\",\"(\\d+)\"");
                Matcher matcher = pattern.matcher(res.body());
                if(matcher.find())
                {
                    String coins = matcher.group(1);//本次签到得到的金币数
                    String days = matcher.group(2);//连续签到的天数
                    this.resultFlag = "true";
                    if(coins.length() == 0)
                    {
                        this.resultStr = "今天已签过到，共打卡" + days + "天";
                    }
                    else
                    {
                        this.resultStr = "签到成功，获得" + coins + "沪元，共打卡" + days + "天";
                    }
                }
                else
                {
                    this.resultFlag = "false";
                    this.resultStr = "登录成功，但签到失败";
                }
            }
            else
            {
                //登录失败
                this.resultFlag = "false";
                this.resultStr = "提交登录信息后服务器返回失败信息，登录失败";
            }
            
            
        } catch (IOException e) {
            this.resultFlag = "false";
            this.resultStr = "访问网页时发生网络错误，登录失败";
            e.printStackTrace();
        } catch (Exception e) {
            this.resultFlag = "false";
            this.resultStr = "未知错误！";
            e.printStackTrace();
        }
        
        return new String[]{resultFlag, resultStr};
    }
    
    
}
