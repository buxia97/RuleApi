package com.RuleApi.common;

import org.springframework.boot.system.ApplicationHome;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditFile {
    /**
     * 替换文本文件中的 非法字符串
     */
    public void replacTextContent(String srcStr,String replaceStr) throws IOException {

        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        /* 配置文件路径 */
        String path = jarF.getParentFile().toString()+"/application.properties";
        // 读
        File file = new File(path);
        FileReader in = new FileReader(file);
        BufferedReader bufIn = new BufferedReader(in);
        // 内存流, 作为临时流
        CharArrayWriter tempStream = new CharArrayWriter();
        // 替换
        String line = null;
        while ((line = bufIn.readLine()) != null) {
            // 替换每行中, 符合条件的字符串
            line = line.replaceAll(srcStr, replaceStr);
            // 将该行写入内存
            tempStream.write(line);
            // 添加换行符
            tempStream.append(System.getProperty("line.separator"));
        }
        // 关闭 输入流
        bufIn.close();
        // 将内存中的流 写入 文件
        FileWriter out = new FileWriter(file);
        tempStream.writeTo(out);
        out.close();
        System.out.println("====path:" + path);
    }
    /**
     * unicode转中文
     * @param str
     * @return
     * @author yutao
     * @date 2017年1月24日上午10:33:25
     */
    public static String unicodeToString(String str) {

        Pattern compile = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");

        Matcher matcher = compile.matcher(str);

        char ch;

        while (matcher.find()) {

            ch = (char) Integer.parseInt(matcher.group(2), 16);

            str = str.replace(matcher.group(1), ch+"" );

        }

        return str;

    }

}
