package com.RuleApi.common;

//常用数据处理类

import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class baseFull {
    //数组去重
    public Object[] threeClear(Object[] arr){
        List list = new ArrayList();
        for(int i=0;i<arr.length;i++){
            if(!list.contains(arr[i])){
                list.add(arr[i]);
            }
        }
        return list.toArray();
    }
    //获取字符串内图片地址
    public List<String> getImageSrc(String htmlCode) {
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(htmlCode);

        while (urlMatcher.find())
        {
            containedUrls.add(htmlCode.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        List<String> imageList = new ArrayList<String>();
        for (int i = 0; i < containedUrls.size(); i++) {
            String word = containedUrls.get(i);
            if(word.indexOf(".jpg") != -1||word.indexOf(".jpeg") != -1||word.indexOf(".png") != -1||word.indexOf(".bmp") != -1||word.indexOf(".gif") != -1){
                imageList.add(word.replaceAll("\\)",""));
            }
        }
        return imageList;
    }
    //获取markdown内图片引用
    public List<String> getImageCode(String htmlCode) {
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(htmlCode);

        while (urlMatcher.find())
        {
            containedUrls.add(htmlCode.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        List<String> codeList = new ArrayList<String>();
        for (int i = 0; i < containedUrls.size(); i++) {
            String word = containedUrls.get(i);

            codeList.add(word);
        }
        return codeList;
    }
    public static boolean isEmail(String string) {
        if (string == null)
            return false;
        String regEx1 = "^([a-z0-9A-Z]+[-_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p;
        Matcher m;
        p = Pattern.compile(regEx1);
        m = p.matcher(string);
        if (m.matches())
            return true;
        else
            return false;
    }
    //获取markdown引用的图片地址
    public List<String> getImageMk(String htmlCode) {
        List<String> containedUrls = new ArrayList<String>();
       // String urlRegex = "\\\\[\\\\d\\\\]:\\\\s(https?|http):((//)|(\\\\\\\\))+[\\\\w\\\\d:#@%/;$()~_?\\\\+-=\\\\\\\\\\\\.&]*";
        String urlRegex = "\\[\\d\\]:\\s(https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(htmlCode);

        while (urlMatcher.find())
        {
            containedUrls.add(htmlCode.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        List<String> imageCode = new ArrayList<String>();
        for (int i = 0; i < containedUrls.size(); i++) {
            String word = containedUrls.get(i);
            if(word.indexOf(".jpg") != -1||word.indexOf(".JPG") != -1||word.indexOf(".jpeg") != -1||word.indexOf(".png") != -1||word.indexOf(".PNG") != -1||word.indexOf(".bmp") != -1||word.indexOf(".gif") != -1||word.indexOf(".GIF") != -1){
                imageCode.add(word.replaceAll("\\)",""));
            }
        }
        return imageCode;
    }
    //获取ip地址
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1")) {
                    // 根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                // = 15
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress="";
        }
        // ipAddress = this.getRequest().getRemoteAddr();

        return ipAddress;
    }
    /**
     * 提取字符串中文字符
     * @param text
     * @return
     */
    public static String toStrByChinese(String text){
        text=text.replaceAll("\\[hide(([\\s\\S])*?)\\[\\/hide\\]", "");
        text=text.replaceAll("\\{hide(([\\s\\S])*?)\\{\\/hide\\}", "");
        text=text.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", "");
        text=text.replaceAll("\\s*", "");
        text=text.replaceAll("</?[^>]+>", "");
        //去掉文章开头的图片插入
        text=text.replaceAll("((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)","");
        text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))", "");
        text=text.replaceAll("((!\\[)[\\s\\S]+?(\\]))", "");
        text=text.replaceAll("\\(", "");
        text=text.replaceAll("\\)", "");
        text=text.replaceAll("\\[", "");
        text=text.replaceAll("\\]", "");
        return text;
    }
    //生成随机英文字符串
    public static String createRandomStr(int length){
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            stringBuffer.append(str.charAt(number));
        }
        return stringBuffer.toString();
    }
    //随机数
    protected long generateRandomNumber(int n){
        if(n<1){
            throw new IllegalArgumentException("随机数位数必须大于0");
        }
        return (long)(Math.random()*9*Math.pow(10,n-1)) + (long)Math.pow(10,n-1);
    }
    //头像获取
    public static String getAvatar(String url,String email){
        String avatar = "";
        String qqUrl = "https://thirdqq.qlogo.cn/g?b=qq&nk=";
        String regex = "[1-9][0-9]{8,10}\\@[q][q]\\.[c][o][m]";
        if(email.matches(regex)){
            String[] qqArr = email.split("@");
            String qq = qqArr[0];
            avatar = qqUrl+ qq+"&s=100";
        }else {
            avatar = url+ DigestUtils.md5DigestAsHex(email.getBytes());
        }
        return avatar;

    }
    //生成lv等级
    public static Integer getLv(Integer num){
        Integer lv = 0;
        try {
            if(num<10){
                lv = 0;
            }else if(num>=10&&num<50){
                lv = 1;
            }else if(num>=50&&num<200){
                lv = 2;
            }else if(num>=200&&num<500){
                lv = 3;
            }else if(num>=500&&num<1000){
                lv = 4;
            }else if(num>=1000&&num<2000){
                lv = 5;
            }else if(num>=2000&&num<5000){
                lv = 6;
            }else if(num>=5000){
                lv = 7;
            }
            return lv;

        }catch (Exception e){
            return 0;
        }
    }
}
