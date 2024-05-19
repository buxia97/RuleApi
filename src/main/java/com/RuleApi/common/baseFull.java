package com.RuleApi.common;

//常用数据处理类

import com.RuleApi.entity.TypechoUsers;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class baseFull {
    //数组去重
    public Object[] threeClear(Object[] arr) {
        List list = new ArrayList();
        for (int i = 0; i < arr.length; i++) {
            if (!list.contains(arr[i])) {
                list.add(arr[i]);
            }
        }
        return list.toArray();
    }

    //获取字符串内图片地址
    public List<String> getImageSrc(String htmlCode) {
        List<String> urls = extractUrls(htmlCode);
        List<String> imageUrls = new ArrayList<>();

        for (String url : urls) {
            if (url.matches(".+\\.(ico|jpe?g|png|bmp|gif|webp|ICO|JPE?G|PNG|BMP|GIF|WEBP)$")) {
                imageUrls.add(url.replaceAll("\\)", ""));
            }
        }

        return imageUrls;
    }

    private List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b(https?|ftp|file)://[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            urls.add(text.substring(matcher.start(0), matcher.end(0)));
        }

        return urls;
    }

    //获取markdown内图片引用
    public List<String> getImageCode(String htmlCode) {
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(htmlCode);

        while (urlMatcher.find()) {
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

        while (urlMatcher.find()) {
            containedUrls.add(htmlCode.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        List<String> imageCode = new ArrayList<String>();
        for (int i = 0; i < containedUrls.size(); i++) {
            String word = containedUrls.get(i);
            if (word.indexOf(".ico") != -1 || word.indexOf(".jpg") != -1 || word.indexOf(".JPG") != -1 || word.indexOf(".jpeg") != -1 || word.indexOf(".png") != -1 || word.indexOf(".PNG") != -1 || word.indexOf(".bmp") != -1 || word.indexOf(".gif") != -1 || word.indexOf(".GIF") != -1 || word.indexOf(".webp") != -1 || word.indexOf(".WEBP") != -1) {
                imageCode.add(word.replaceAll("\\)", ""));
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
            ipAddress = "";
        }
        // ipAddress = this.getRequest().getRemoteAddr();

        return ipAddress;
    }

    /**
     * 提取字符串中文字符
     *
     * @param text
     * @return
     */
    public static String toStrByChinese(String text) {
        text = text.replaceAll("\\[hide(([\\s\\S])*?)\\[\\/hide\\]", "");
        text = text.replaceAll("\\{hide(([\\s\\S])*?)\\{\\/hide\\}", "");
        text = text.replaceAll("(\\\r\\\n|\\\r|\\\n|\\\n\\\r)", "");
        text = text.replaceAll("\\s*", "");
        text = text.replaceAll("</?[^>]+>", "");
        //去掉文章开头的图片插入
        text = text.replaceAll("((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", "");
        text = text.replaceAll("((!\\[)[\\s\\S]+?(\\]\\[)[\\s\\S]+?(\\]))", "");
        text = text.replaceAll("((!\\[)[\\s\\S]+?(\\]))", "");
        text = text.replaceAll("\\(", "");
        text = text.replaceAll("\\)", "");
        text = text.replaceAll("\\[", "");
        text = text.replaceAll("\\]", "");
        return text;
    }

    //生成随机英文字符串
    public static String createRandomStr(int length) {
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
    protected long generateRandomNumber(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("随机数位数必须大于0");
        }
        return (long) (Math.random() * 9 * Math.pow(10, n - 1)) + (long) Math.pow(10, n - 1);
    }

    //头像获取
    public static String getAvatar(String url, String email) {
        String avatar = "";
        String qqUrl = "https://thirdqq.qlogo.cn/g?b=qq&nk=";
        String regex = "[1-9][0-9]{8,10}\\@[q][q]\\.[c][o][m]";
        if (email.matches(regex)) {
            String[] qqArr = email.split("@");
            String qq = qqArr[0];
            avatar = qqUrl + qq + "&s=100";
        } else {
            avatar = url + DigestUtils.md5DigestAsHex(email.getBytes());
        }
        return avatar;

    }
    //判断是否有敏感代码
    public Integer haveCode(String text) {
        try {
            if (text.indexOf("<script>") != -1) {
                return 1;
            }
            if (text.indexOf("eval(") != -1) {
                return 1;
            }
            if (text.indexOf("<iframe>") != -1) {
                return 1;
            }
            if (text.indexOf("<frame>") != -1) {
                return 1;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }

    }
    //生成lv等级
    public static Integer getLv(Integer num) {
        Integer lv = 0;
        try {
            if (num < 10) {
                lv = 0;
            } else if (num >= 10 && num < 50) {
                lv = 1;
            } else if (num >= 50 && num < 200) {
                lv = 2;
            } else if (num >= 200 && num < 500) {
                lv = 3;
            } else if (num >= 500 && num < 1000) {
                lv = 4;
            } else if (num >= 1000 && num < 2000) {
                lv = 5;
            } else if (num >= 2000 && num < 5000) {
                lv = 6;
            } else if (num >= 5000) {
                lv = 7;
            }
            return lv;

        } catch (Exception e) {
            return 0;
        }
    }
    public static Integer isVideo(String type){
        String lowerCaseType = type.toLowerCase();
        if (lowerCaseType.equals(".mp4") || lowerCaseType.equals(".avi") || lowerCaseType.equals(".mkv")) {
            return 1; // 是视频
        } else {
            return 0; // 不是视频
        }
    }
    public static Integer isMedia(String type){
        String lowerCaseType = type.toLowerCase();
        if (lowerCaseType.equals(".mp4") || lowerCaseType.equals(".avi") || lowerCaseType.equals(".mkv") || lowerCaseType.equals(".mp3") || lowerCaseType.equals(".wav")) {
            return 1; // 是媒体文件
        } else {
            return 0; // 不是媒体文件
        }
    }


    public Integer getForbidden(String forbidden, String text) {
        Integer isForbidden = 0;

        if (forbidden != null && !forbidden.isEmpty()) {
            if (forbidden.contains(",")) {
                String[] strArray = forbidden.split(",");
                for (String str : strArray) {
                    if (str != null && !str.isEmpty()) {
                        if (text.contains(str)) {
                            isForbidden = 1;
                            break; // 如果匹配到一个违禁词就立即停止循环
                        }
                    }
                }
            } else {
                if (text.contains(forbidden) || text.equals(forbidden)) {
                    isForbidden = 1;
                }
            }
        }

        return isForbidden;
    }

    // 加密字符串
    public String encrypt(String plainText) {
        byte[] encodedBytes = Base64.getEncoder().encode(plainText.getBytes());
        return new String(encodedBytes);
    }

    // 解密字符串
    public String decrypt(String encryptedText) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText.getBytes());
        return new String(decodedBytes);
    }

    //从富文本提取存文本
    public String htmlToText(String text) {
        text = text.replaceAll("\\<.*?\\>", "");
        return text;
    }

    /**
     * 利用java原生的摘要实现SHA256加密
     * @param str 加密后的报文
     * @return
     */
    public String getSHA256StrJava(String str){
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes("UTF-8"));
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodeStr;
    }
    /**
     * 将byte转为16进制
     * @param bytes
     * @return
     */
    private String byte2Hex(byte[] bytes){
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i=0;i<bytes.length;i++){
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length()==1){
                //1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    public int getTextLength(String text) {
        // 使用正则表达式去除HTML标签
        String noHtml = text.replaceAll("<[^>]*>", "");
        // 计算并返回去除HTML标签后的字符串长度
        int length = noHtml.length();
        return length;
    }

    public List<String> getImageBase64(java.lang.String htmlCode) {
        List<java.lang.String> srcList = new ArrayList<>();
        // 定义正则表达式来匹配<img>标签的src属性
        Pattern imgPattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        Matcher matcher = imgPattern.matcher(htmlCode);

        // 查找匹配项
        while (matcher.find()) {
            java.lang.String src = matcher.group(1); // 提取src值
            // 判断src值是否为Base64数据
            if (src.startsWith("data:image/")) {
                srcList.add(src); // 如果是Base64数据，才加入列表
            }
        }
        return srcList;
    }

    public static InputStream convertHtmlToDocx(String html) throws IOException, InvalidFormatException {
        XWPFDocument doc = new XWPFDocument();
        Document document = Jsoup.parse(html);

        // 遍历所有元素
        for (Element element : document.body().children()) {
            parseElement(element, doc, doc.createParagraph());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static void parseElement(Element element, XWPFDocument doc, XWPFParagraph paragraph) throws IOException, InvalidFormatException {
        XWPFRun run = paragraph.createRun();

        // 处理文本和样式
        switch (element.tagName()) {
            case "p":
                for (Node node : element.childNodes()) {
                    if (node instanceof Element) {
                        Element childElement = (Element) node;
                        handleStyledText(childElement, run);
                    } else {
                        run.setText(node.toString(), 0); // 处理普通文本
                    }
                }
                break;
            case "h1":
            case "h2":
            case "h3":
                run.setBold(true);
                run.setText(element.text());
                break;
            case "img":
                String imgUrl = element.attr("src");
                addImageToDocument(doc, paragraph, imgUrl);
                break;
            // 其他标签...
        }

        // 添加新的段落
        if (!element.tagName().equals("img")) { // 避免图片后立即跟一个新段落
            paragraph = doc.createParagraph();
        }
    }

    private static void handleStyledText(Element element, XWPFRun run) {
        switch (element.tagName()) {
            case "strong":
                run.setBold(true);
                break;
            case "em":
                run.setItalic(true);
                break;
            case "u":
                run.setUnderline(UnderlinePatterns.SINGLE);
                break;
        }
        run.setText(element.text());
    }



    private static void addImageToDocument(XWPFDocument doc, XWPFParagraph paragraph, String imgUrl) throws IOException, InvalidFormatException {
        URL url = new URL(imgUrl);
        URLConnection urlConnection = url.openConnection();
        try (InputStream imgStream = urlConnection.getInputStream()) {
            XWPFRun imgRun = paragraph.createRun();

            // 确定图片格式。这里假设图片是JPEG格式。
            // 注意：你可能需要根据实际图片类型来动态确定这个值。
            PictureData.PictureType pictureType = PictureData.PictureType.JPEG;

            // 添加图片到文档。注意：枚举PictureType的ordinal方法返回枚举常量的顺序，但这不是添加图片的推荐方法。
            // 相反，应该直接使用pictureType的值。
            imgRun.addPicture(imgStream, pictureType.ordinal(), imgUrl, Units.toEMU(300), Units.toEMU(300));
        }
    }
}
