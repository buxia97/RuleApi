package com.RuleApi.common;

//常用数据处理类

import java.util.ArrayList;
import java.util.List;
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
                imageList.add(word);
            }
        }
        return imageList;
    }
}
